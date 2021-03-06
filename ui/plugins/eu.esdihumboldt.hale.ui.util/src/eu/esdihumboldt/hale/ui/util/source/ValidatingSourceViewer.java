/*
 * Copyright (c) 2013 Data Harmonisation Panel
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Data Harmonisation Panel <http://www.dhpanel.eu>
 */

package eu.esdihumboldt.hale.ui.util.source;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;

import de.fhg.igd.slf4jplus.ALogger;
import de.fhg.igd.slf4jplus.ALoggerFactory;
import eu.esdihumboldt.hale.ui.util.jobs.ExclusiveSchedulingRule;

/**
 * Source viewer that validates its content on document changes asynchronously
 * in a Job.
 * 
 * @author Simon Templer
 */
public class ValidatingSourceViewer extends SourceViewer {

	private static final ALogger log = ALoggerFactory.getLogger(ValidatingSourceViewer.class);

	/**
	 * Validation scheduling delay in milliseconds.
	 */
	private static final int VALIDATE_DELAY = 500;

	private final AtomicBoolean validationEnabled = new AtomicBoolean(true);

	private final ReentrantLock changeLock = new ReentrantLock();

	/**
	 * If the document has changed since validation. Protected by lock.
	 */
	private boolean changed = true;

	/**
	 * If the document was valid in the last validation run.
	 */
	private boolean valid = false;

	private IDocumentListener documentListener;

	private IDocument lastDocument;

	private Job validateJob;

	/**
	 * Name of the property holding the state if the viewer's document is valid.
	 */
	public static final String PROPERTY_VALID = "valid";

	/**
	 * Name of the property holding the state if the viewer validation is
	 * enabled.
	 */
	public static final String PROPERTY_VALIDATION_ENABLED = "validationEnabled";

	private final Set<IPropertyChangeListener> propertyChangeListeners = new CopyOnWriteArraySet<>();

	private final SourceValidator validator;

	/**
	 * Constructs a new validating source viewer.
	 * 
	 * @param parent the parent of the viewer's control
	 * @param ruler the vertical ruler used by this source viewer
	 * @param styles the SWT style bits for the viewer's control
	 * @param validator the source validator
	 */
	public ValidatingSourceViewer(Composite parent, IVerticalRuler ruler, int styles,
			SourceValidator validator) {
		super(parent, ruler, styles);
		this.validator = validator;

		init();
	}

	/**
	 * Constructs a new validating source viewer.
	 * 
	 * @param parent the parent of the viewer's control
	 * @param verticalRuler the vertical ruler used by this source viewer
	 * @param overviewRuler the overview ruler
	 * @param showAnnotationsOverview <code>true</code> if the overview ruler
	 *            should be visible, <code>false</code> otherwise
	 * @param styles the SWT style bits for the viewer's control
	 * @param validator the source validator
	 */
	public ValidatingSourceViewer(Composite parent, IVerticalRuler verticalRuler,
			IOverviewRuler overviewRuler, boolean showAnnotationsOverview, int styles,
			SourceValidator validator) {
		super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles);
		this.validator = validator;

		init();
	}

	/**
	 * Validate the document content. The default implementation always returns
	 * <code>true</code>.
	 * 
	 * @param content the document content
	 * @return if the content is valid
	 */
	protected final boolean validate(String content) {
		return validator.validate(content);
	}

	/**
	 * Initialize the Job and listener.
	 */
	protected void init() {
		validateJob = new Job("Source viewer validation") {

			@Override
			public boolean shouldRun() {
				return validationEnabled.get();
			}

			@Override
			public boolean shouldSchedule() {
				return validationEnabled.get();
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				String content;
				changeLock.lock();
				try {
					if (!changed) {
						return Status.OK_STATUS;
					}
					IDocument doc = getDocument();
					if (doc != null) {
						content = doc.get();
					}
					else {
						content = "";
					}
					changed = false;
				} finally {
					changeLock.unlock();
				}

				boolean success = false;
				try {
					// this is the potentially long running stuff
					success = validate(content);
				} catch (Exception e) {
					// ignore, but log
					log.warn("Error validating document content", e);
					success = false;
				}

				boolean notify = false;
				changeLock.lock();
				try {
					/*
					 * Only notify listeners if the document was not changed in
					 * the meantime and the valid state is different than
					 * before.
					 */
					notify = !changed && valid != success;
					if (notify) {
						// set result
						valid = success;
					}
				} finally {
					changeLock.unlock();
				}

				if (notify) {
					PropertyChangeEvent event = new PropertyChangeEvent(
							ValidatingSourceViewer.this, PROPERTY_VALID, !success, success);
					notifyOnPropertyChange(event);
				}

				return Status.OK_STATUS;
			}
		};
		validateJob.setUser(false);
		validateJob.setRule(new ExclusiveSchedulingRule(this));

		documentListener = new IDocumentListener() {

			@Override
			public void documentChanged(DocumentEvent event) {
				scheduleValidation();
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
				// do nothing
			}
		};
	}

	/**
	 * Notify listeners on a property change event.
	 * 
	 * @param event the event
	 */
	protected void notifyOnPropertyChange(PropertyChangeEvent event) {
		for (IPropertyChangeListener listener : propertyChangeListeners) {
			try {
				listener.propertyChange(event);
			} catch (Exception e) {
				log.error("Error notifying listener on property change", e);
			}
		}
	}

	/**
	 * Force new validation.
	 */
	public void forceUpdate() {
		scheduleValidation();
	}

	/**
	 * Schedule the validation job.
	 */
	protected void scheduleValidation() {
		changeLock.lock();
		try {
			changed = true;
		} finally {
			changeLock.unlock();
		}

		// schedule validation
		validateJob.schedule(VALIDATE_DELAY);
	}

	/**
	 * Get the result of the last validation.
	 * 
	 * @return the validation
	 */
	public boolean isValid() {
		changeLock.lock();
		try {
			return valid;
		} finally {
			changeLock.unlock();
		}
	}

	@Override
	public void setDocument(IDocument document) {
		super.setDocument(document);

		updateListener(document);
	}

	private void updateListener(IDocument document) {
		if (lastDocument != document) {
			if (lastDocument != null) {
				lastDocument.removeDocumentListener(documentListener);
			}

			if (document != null) {
				document.addDocumentListener(documentListener);
			}
			lastDocument = document;

			if (document != null) {
				// initial validation
				scheduleValidation();
			}
		}
	}

	@Override
	public void setDocument(IDocument document, int visibleRegionOffset, int visibleRegionLength) {
		super.setDocument(document, visibleRegionOffset, visibleRegionLength);

		updateListener(document);
	}

	@Override
	public void setDocument(IDocument document, IAnnotationModel annotationModel) {
		super.setDocument(document, annotationModel);

		updateListener(document);
	}

	@Override
	public void setDocument(IDocument document, IAnnotationModel annotationModel,
			int modelRangeOffset, int modelRangeLength) {
		super.setDocument(document, annotationModel, modelRangeOffset, modelRangeLength);

		updateListener(document);
	}

	/**
	 * Add a property change listener. It will be notified on changes to the
	 * {@value #PROPERTY_VALID} property.
	 * 
	 * @param listener the listener to add
	 */
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		propertyChangeListeners.add(listener);
	}

	/**
	 * Remove a property change listener.
	 * 
	 * @param listener the listener to add
	 */
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		propertyChangeListeners.remove(listener);
	}

	/**
	 * @return if the validation is currently enabled
	 */
	public boolean isValidationEnabled() {
		return validationEnabled.get();
	}

	/**
	 * Enable or disable the automatic validation.
	 * 
	 * @param enabled <code>true</code> if the validation should be enabled,
	 *            <code>false</code> if it should be disabled
	 */
	public void setValidationEnabled(boolean enabled) {
		boolean old = validationEnabled.getAndSet(enabled);
		if (old != enabled) {
			PropertyChangeEvent event = new PropertyChangeEvent(ValidatingSourceViewer.this,
					PROPERTY_VALIDATION_ENABLED, old, enabled);
			notifyOnPropertyChange(event);

			if (enabled) {
				// force validation
				forceUpdate();
			}
		}
	}

}
