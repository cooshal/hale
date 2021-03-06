/*
 * Copyright (c) 2012 Data Harmonisation Panel
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
 *     HUMBOLDT EU Integrated Project #030962
 *     Data Harmonisation Panel <http://www.dhpanel.eu>
 */
package eu.esdihumboldt.hale.ui.views.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeColumnViewerLabelProvider;
import org.eclipse.jface.viewers.TreeNode;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.part.WorkbenchPart;

import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.tasks.ResolvedTask;
import eu.esdihumboldt.hale.common.tasks.Task;
import eu.esdihumboldt.hale.common.tasks.TaskService;
import eu.esdihumboldt.hale.common.tasks.TaskServiceListener;
import eu.esdihumboldt.hale.ui.HaleUI;
import eu.esdihumboldt.hale.ui.service.tasks.TaskServiceAdapter;
import eu.esdihumboldt.hale.ui.util.tree.CollectionTreeNodeContentProvider;
import eu.esdihumboldt.hale.ui.util.tree.DefaultTreeNode;
import eu.esdihumboldt.hale.ui.util.tree.MapTreeNode;
import eu.esdihumboldt.hale.ui.util.tree.SortedMapTreeNode;
import eu.esdihumboldt.hale.ui.util.viewer.ReadOnlyEditingSupport;
import eu.esdihumboldt.hale.ui.views.mapping.MappingView;
import eu.esdihumboldt.hale.ui.views.properties.PropertiesViewPart;
import eu.esdihumboldt.hale.ui.views.tasks.internal.Messages;

/**
 * Task view based on a tree
 * 
 * @author Simon Templer
 * @partner 01 / Fraunhofer Institute for Computer Graphics Research
 */
public class TaskTreeView extends ViewPart {

	/**
	 * The view ID
	 */
	public static String ID = "eu.esdihumboldt.hale.ui.views.tasks"; //$NON-NLS-1$

	/**
	 * The tree viewer
	 */
	private TreeViewer tree;
	private TaskService taskService;

	private TaskServiceListener taskListener;
	@SuppressWarnings("unused")
	private ISelectionListener selectionListener;

	private MapTreeNode<Cell, MapTreeNode<ResolvedTask<Cell>, TreeNode>> cellNode;
	private final Map<Task<?>, DefaultTreeNode> taskNodes = new HashMap<Task<?>, DefaultTreeNode>();

	/**
	 * @see WorkbenchPart#createPartControl(Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		Composite page = new Composite(parent, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		page.setLayoutData(data);

		// tree column layout
		TreeColumnLayout layout = new TreeColumnLayout();
		page.setLayout(layout);

		// tree viewer
		tree = new TreeViewer(page, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
		tree.setContentProvider(new CollectionTreeNodeContentProvider());
		tree.setUseHashlookup(true);

		tree.getTree().setHeaderVisible(true);
		tree.getTree().setLinesVisible(true);

		tree.setComparator(new TaskTreeComparator());

		taskService = HaleUI.getServiceProvider().getService(TaskService.class);

		// columns
		IColorProvider colorProvider = new TaskColorProvider(Display.getCurrent());

		// title/description
		TreeViewerColumn description = new TreeViewerColumn(tree, SWT.LEFT);
		description.getColumn().setText(Messages.TaskTreeView_TitleDescriptionText);
		description.getColumn().setToolTipText(Messages.TaskTreeView_description_tooltip);
		TreeColumnViewerLabelProvider descriptionLabelProvider = new TreeColumnViewerLabelProvider(
				new TaskDescriptionLabelProvider(0));
		descriptionLabelProvider.setProviders(colorProvider);
		description.setLabelProvider(descriptionLabelProvider);
		description.setEditingSupport(new ReadOnlyEditingSupport(tree, descriptionLabelProvider));
		layout.setColumnData(description.getColumn(), new ColumnWeightData(4));

		// number of tasks
		TreeViewerColumn number = new TreeViewerColumn(tree, SWT.RIGHT);
		number.getColumn().setText("#"); //$NON-NLS-1$
		number.getColumn().setToolTipText(Messages.TaskTreeView_NumberText);
		TreeColumnViewerLabelProvider numberLabelProvider = new TreeColumnViewerLabelProvider(
				new TaskCountLabelProvider(0));
		numberLabelProvider.setProviders(colorProvider);
		number.setLabelProvider(numberLabelProvider);
		layout.setColumnData(number.getColumn(), new ColumnWeightData(0, 48));

		// user data: status
		TreeViewerColumn status = new TreeViewerColumn(tree, SWT.LEFT);
		status.getColumn().setText(Messages.TaskTreeView_StatusText);
		TreeColumnViewerLabelProvider statusLabelProvider = new TreeColumnViewerLabelProvider(
				new TaskStatusLabelProvider(0));
		statusLabelProvider.setProviders(colorProvider);
		status.setLabelProvider(statusLabelProvider);
		layout.setColumnData(status.getColumn(), new ColumnWeightData(1));
		status.setEditingSupport(new TaskStatusEditingSupport(tree, taskService));

		// user data: comment
//		TreeViewerColumn comment = new TreeViewerColumn(tree, SWT.LEFT);
//		comment.getColumn().setText(Messages.TaskTreeView_CommentText);
//		TreeColumnViewerLabelProvider commentLabelProvider = new TreeColumnViewerLabelProvider(
//				new TaskCommentLabelProvider(0));
//		commentLabelProvider.setProviders(colorProvider);
//		comment.setLabelProvider(commentLabelProvider);
//		layout.setColumnData(comment.getColumn(), new ColumnWeightData(4));
//		comment.setEditingSupport(new TaskCommentEditingSupport(tree, taskService));

		// listeners
		taskService.addListener(taskListener = new TaskServiceAdapter() {

			@Override
			public void tasksRemoved(final Iterable<Task<?>> tasks) {
				if (Display.getCurrent() != null) {
					removeTasks(tasks);
				}
				else {
					final Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {

						@Override
						public void run() {
							removeTasks(tasks);
						}

					});
				}
			}

			@Override
			public void tasksAdded(final Iterable<Task<?>> tasks) {
				if (Display.getCurrent() != null) {
					addTasks(tasks);
				}
				else {
					final Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {

						@Override
						public void run() {
							addTasks(tasks);
						}

					});
				}
			}

			@Override
			public void taskUserDataChanged(final ResolvedTask<?> task) {
				if (Display.getCurrent() != null) {
					updateNode(task);
				}
				else {
					final Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {

						@Override
						public void run() {
							updateNode(task);
						}

					});
				}
			}

		});

		getSite().getWorkbenchWindow().getSelectionService()
				.addPostSelectionListener(selectionListener = new ISelectionListener() {

					@Override
					public void selectionChanged(IWorkbenchPart part, ISelection selection) {
						if (!(part instanceof PropertiesViewPart)) {
							// only update the selection if it originates from a
							// part that provides definition or instance
							// selections
							return;
						}

						if (part != TaskTreeView.this) {
							update(selection);
						}
					}
				});

		createInput();

		configureActions();

		// interaction
		tree.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				ISelection selection = event.getSelection();
				if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
					Object selected = ((IStructuredSelection) selection).getFirstElement();
					if (selected instanceof TreeNode) {
						// determine value
						Object tmp = ((TreeNode) selected).getValue();
						Object value;
						if (tmp.getClass().isArray()) {
							value = ((Object[]) tmp)[0];
						}
						else {
							value = tmp;
						}
						if (value instanceof Task) {
							// node is task node
							Task<?> task = (Task<?>) value;
							onDoubleClick(task.getMainContext());
						}
						else if (value instanceof Cell) {
							onDoubleClick(value);
						}
					}
				}
			}
		});
	}

	/**
	 * React on a double click on an item that represents the given identifier
	 * 
	 * @param context the context object of the double-clicked node
	 */
	protected void onDoubleClick(Object context) {
		String cellId;
		if (context instanceof Cell) {
			cellId = ((Cell) context).getId();
		}
		else {
			return;
		}

		IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		try {
			IViewPart part = activeWindow.getActivePage().showView(MappingView.ID);
			if (part instanceof MappingView) {
				((MappingView) part).selectCell(cellId);
			}
		} catch (PartInitException e) {
			// ignore
		}

	}

	private void configureActions() {
		// TODO Add menu to filter tasks

//		IActionBars bars = getViewSite().getActionBars();
//
//		// menu
//		IMenuManager menu = bars.getMenuManager();
//		menu.add(new TaskProviderMenu());
	}

	/**
	 * Update the view
	 */
	private void createInput() {
		TaskService taskService = HaleUI.getServiceProvider().getService(TaskService.class);

		final Collection<TreeNode> input = new ArrayList<TreeNode>();

		cellNode = new MapTreeNode<Cell, MapTreeNode<ResolvedTask<Cell>, TreeNode>>(
				"Cell messages");
		input.add(cellNode);

		Collection<ResolvedTask<?>> tasks = taskService.getResolvedTasks();
		for (ResolvedTask<?> task : tasks) {
			addTask(task);
		}

		tree.setInput(input);
	}

	/**
	 * Add a resolved task
	 * 
	 * @param task the task to add
	 */
	@SuppressWarnings("unchecked")
	private <C> void addTask(ResolvedTask<C> task) {
		// add task to model
		MapTreeNode<ResolvedTask<C>, TreeNode> parent = (MapTreeNode<ResolvedTask<C>, TreeNode>) getParentNode(
				task, true);
		if (parent != null) {
			DefaultTreeNode node = new DefaultTreeNode(task);
			parent.addChild(task, node);
			taskNodes.put(task.getTask(), node);
			// update viewer
			tree.refresh(parent, true);
			// update icons
			TreeNode updateNode = parent.getParent();
			while (updateNode != null) {
				tree.update(updateNode, null);
				updateNode = updateNode.getParent();
			}
		}
	}

	/**
	 * Adds the given tasks
	 * 
	 * @param tasks the tasks to add
	 */
	protected void removeTasks(Iterable<Task<?>> tasks) {
		// TODO smart refresh
		for (Task<?> task : tasks) {
			removeTask(task);
		}
	}

	/**
	 * Remove a task
	 * 
	 * @param task the task to remove
	 */
	@SuppressWarnings("unchecked")
	private void removeTask(Task<?> task) {
		DefaultTreeNode node = taskNodes.get(task);

		if (node != null) {
			// remove task from model
			MapTreeNode<ResolvedTask<?>, TreeNode> parent = (MapTreeNode<ResolvedTask<?>, TreeNode>) node
					.getParent();
			if (parent != null) {
				parent.removeChildNode(node);
				taskNodes.remove(task);
				// remove empty nodes
				if (!parent.hasChildren()) {
					MapTreeNode<Cell, MapTreeNode<ResolvedTask<?>, TreeNode>> root = (MapTreeNode<Cell, MapTreeNode<ResolvedTask<?>, TreeNode>>) parent
							.getParent();
					root.removeChildNode(parent);
					tree.refresh(root, true);
				}
				else {
					tree.refresh(parent, true);
					// update icons
					TreeNode updateNode = parent.getParent();
					while (updateNode != null) {
						tree.update(updateNode, null);
						updateNode = updateNode.getParent();
					}
				}
			}
		}
	}

	/**
	 * Update the node label for the given task
	 * 
	 * @param task the task
	 */
	protected void updateNode(ResolvedTask<?> task) {
		DefaultTreeNode node = taskNodes.get(task.getTask());
		if (node != null) {
			node.setValues(task);
			// refresh parent instead of update node (sorting) -
			// tree.update(node, null);

			// update parent nodes
			TreeNode parent = node.getParent();

			if (parent != null) {
				tree.refresh(parent, true);
				parent = parent.getParent();
			}
			else {
				tree.update(node, null);
			}

			while (parent != null) {
				tree.update(parent, null);
				parent = parent.getParent();
			}
		}
	}

	/**
	 * Get the parent node for the given task
	 * 
	 * @param task the task
	 * @param allowCreate allow creating the node if it does not yet exist
	 * 
	 * @return the parent node
	 */
	private <C> MapTreeNode<?, TreeNode> getParentNode(ResolvedTask<C> task, boolean allowCreate) {

		if (task.getMainContext() instanceof Cell) {
			MapTreeNode<ResolvedTask<Cell>, TreeNode> node = getGroupNode(cellNode,
					(Cell) task.getMainContext(), allowCreate);
			return node;
		}
//		Object group = TaskUtils.getGroup(task);
//		if (group.getName().getNamespaceURI().equals(schemaService.getSourceNameSpace())) {
//			// source task
//			return getGroupNode(sourceNode, group, allowCreate);
//		}
//		else if (group.getElementName().getNamespaceURI()
//				.equals(schemaService.getTargetNameSpace())) {
//			// target task
//			return getGroupNode(targetNode, group, allowCreate);
//		}
//		else {
//			// invalid task ?
//			MapTreeNode<ResolvedTask, TreeNode> result = getGroupNode(sourceNode, null, true);
//			if (result == null) {
//				result = getGroupNode(targetNode, null, true);
//			}
//			return result;
//		}

		return null;

//		sourceNode.addChild(key, child);
	}

	/**
	 * Get the group node for the group defined by the given type definition
	 * 
	 * @param rootNode the root node
	 * @param group the group's type definition
	 * @param allowCreate allow creating the node if it does not yet exist
	 * 
	 * @return the group node
	 */
	private <C> MapTreeNode<ResolvedTask<C>, TreeNode> getGroupNode(
			MapTreeNode<C, MapTreeNode<ResolvedTask<C>, TreeNode>> rootNode, C group,
			boolean allowCreate) {
		MapTreeNode<ResolvedTask<C>, TreeNode> groupNode = rootNode.getChild(group);
		if (groupNode == null && allowCreate) {
			groupNode = new SortedMapTreeNode<ResolvedTask<C>, TreeNode>(group);
			rootNode.addChild(group, groupNode);
			// update viewer
			tree.refresh(rootNode, true);
		}
		return groupNode;
	}

	/**
	 * Adds the given tasks
	 * 
	 * @param tasks the tasks to add
	 */
	protected void addTasks(Iterable<Task<?>> tasks) {
		// TODO smart refresh
		for (Task<?> task : tasks) {
			addTask(taskService.resolveTask(task));
		}
	}

	/**
	 * Update the view with the given selection
	 * 
	 * @param selection the selection
	 */
	public void update(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection).getFirstElement();
			if (element instanceof Cell) {
				Collection<Task<Cell>> tasks = taskService.getTasks((Cell) element);
				if (!tasks.isEmpty()) {
					ResolvedTask<Cell> rt = taskService.resolveTask(tasks.iterator().next());
					MapTreeNode<?, TreeNode> cellNode = getParentNode(rt, false);
					tree.setSelection(new StructuredSelection(cellNode));
					tree.expandToLevel(cellNode, TreeViewer.ALL_LEVELS);
				}
			}
		}
		else {
			tree.setInput(null);
		}
	}

	/**
	 * @see WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		tree.getControl().setFocus();
	}

	/**
	 * @see WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		if (taskListener != null) {
			TaskService taskService = HaleUI.getServiceProvider().getService(TaskService.class);
			taskService.removeListener(taskListener);
		}

		super.dispose();
	}

}
