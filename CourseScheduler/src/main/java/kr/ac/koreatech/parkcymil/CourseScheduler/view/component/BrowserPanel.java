package kr.ac.koreatech.parkcymil.CourseScheduler.view.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;

import kr.ac.koreatech.parkcymil.CourseScheduler.entity.AppData;
import kr.ac.koreatech.parkcymil.CourseScheduler.entity.Basket;
import kr.ac.koreatech.parkcymil.CourseScheduler.entity.Browser;
import kr.ac.koreatech.parkcymil.CourseScheduler.entity.Course;
import kr.ac.koreatech.parkcymil.CourseScheduler.entity.Department;
import kr.ac.koreatech.parkcymil.CourseScheduler.view.listener.DepartmentSelectListener;
import kr.ac.koreatech.parkcymil.CourseScheduler.view.listener.SearchEventHandler;
import kr.ac.koreatech.parkcymil.CourseScheduler.view.listener.TableActionHandler;

public class BrowserPanel extends AppPanel {
	
	private static final long serialVersionUID = -5838395870111051633L;
	private Dimension size = new Dimension(700, 200);
	private Browser browser;
	private Basket basket;
	private JTable table;
	private TimeTableModel ttModel;
	private TableRowSorter<TimeTableModel> sorter;
	private RowFilter<TimeTableModel, Integer> searchFilter = null;
	private RowFilter<TimeTableModel, Integer> departmentFilter = null;
	private JTextField searchFld = createSearchField();
	private JButton pickBtn;
	private Insets btnMargin = new Insets(0, 0, 0, 0);

	protected BrowserPanel(Browser browser, Basket basket) {
		setLayout(null);
		setBackground(Color.LIGHT_GRAY);
		setMinimumSize(size);
		setPreferredSize(size);
		
		this.browser = browser;
		this.basket = basket;
		JScrollPane tablePane = createTable();
		JLabel searchTxt = createSearchLabel();
		JButton searchBtn = createSearchButton();
		JLabel deptTxt = createDepartmentLabel();
		JComboBox<String> deptBox = createDepartmentComboBox();
		pickBtn = createPickButton();
		
		updateComponents();
		addAll(searchTxt, searchFld, searchBtn, deptTxt, deptBox, pickBtn, tablePane);
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				int width = e.getComponent().getWidth();
				int height = e.getComponent().getHeight();
				tablePane.setBounds(0, 50, width, height - 50);
				tablePane.updateUI();
				updateComponents();
			}
		});
	}
	
	public JTextField getSearchField() {
		return searchFld;
	}
	
	public void setSearchFilter(RowFilter<TimeTableModel, Integer> filter) {
		searchFilter = filter;
		updateFilter();
	}
	
	public void setDepartmentFilter(RowFilter<TimeTableModel, Integer> filter) {
		departmentFilter = filter;
		updateFilter();
	}
	
	private void updateFilter() {
		sorter.setRowFilter(new RowFilter<TimeTableModel, Integer>() {
			@Override
			public boolean include(Entry<? extends TimeTableModel, ? extends Integer> entry) {
				boolean f1 = (searchFilter != null) ? searchFilter.include(entry) : true;
				boolean f2 = (departmentFilter != null) ? departmentFilter.include(entry) : true;
				return f1 && f2;
			}
		});
	}
	
	private void updateComponents() {
		pickBtn.setBounds(getWidth() - 130, 10, 100, 30);
	}
	
	private JTextField createSearchField() {
		JTextField field = new JTextField(10);
		SearchEventHandler handler = new SearchEventHandler(this);
		field.setBounds(60, 10, 200, 30);
		field.addKeyListener(handler.getKeyAdapter());
		return field;
	}
	
	private JLabel createSearchLabel() {
		JLabel label = new JLabel("검색");
		label.setBounds(0, 10, 50, 30);
		label.setHorizontalAlignment(JLabel.RIGHT);
		return label;
	}
	
	private JButton createSearchButton() {
		// Icon by Chanut
		ImageIcon icon = getIcon("search.png", 20);
		JButton button = new JButton(icon);
		SearchEventHandler handler = new SearchEventHandler(this);
		button.setBounds(260, 10, 30, 30);
		button.setMargin(btnMargin);
		button.addMouseListener(handler.getMouseAdapter());
		return button;
	}
	
	private JLabel createDepartmentLabel() {
		JLabel label = new JLabel("학부");
		label.setBounds(300, 10, 50, 30);
		label.setHorizontalAlignment(JLabel.RIGHT);
		return label;
	}
	
	private JComboBox<String> createDepartmentComboBox() {
		JComboBox<String> box = new JComboBox<>();
		box.setBounds(360, 10, 160, 30);
		box.addActionListener(new DepartmentSelectListener(this));
		box.addItem("전체");
		
		for (Department m : Department.values())
			box.addItem(m.getText());
		
		return box;
	}
	
	private JButton createPickButton() {
		JButton button = new JButton("과목 담기");
		TableActionHandler handler = getPickHandler();
		button.setMargin(btnMargin);
		button.addMouseListener(handler.getButtonListener());
		return button;
	}
	
	private JScrollPane createTable() {
		List<Course> courseList = AppData.get().courseList;
		ttModel = new TimeTableModel(courseList);
		sorter = new TableRowSorter<>(ttModel);
		table = new JTable(ttModel);
		JScrollPane pane = new JScrollPane(table);
		TableActionHandler pickHandler = getPickHandler();
		TableActionHandler peekHandler = getPeekHandler();
		
		ttModel.resizeColumnWidth(table.getColumnModel());
		table.setRowSorter(sorter);
		table.setFillsViewportHeight(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getTableHeader().setReorderingAllowed(false);
		table.addMouseListener(pickHandler.getCellDoubleClickListener());
		table.addMouseListener(peekHandler.getCellClickListener());
		table.addMouseListener(getEraseHandler(table));
		return pane;
	}
	
	private TableActionHandler getPickHandler() {
		return new TableActionHandler(table, ttModel) {
			
			@Override
			public void onAction(Course c) {
				Course obstacle = basket.getCourseInConflict(c);
				
				if (obstacle != null) {
					String obsName = new StringBuilder()
							.append(obstacle.getName())
							.append(" ")
							.append(obstacle.getSection())
							.append(" 분반")
							.toString();
					String title = "Time conflicts";
					String message = "That course overlaps with " + obsName;
					JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
				} else {
					basket.pick(c);
				}
			}
			
		};
	}
	
	private TableActionHandler getPeekHandler() {
		return new TableActionHandler(table, ttModel) {

			@Override
			public void onAction(Course c) {
				browser.peek(c);
			}
			
		};
	}
	
	private MouseAdapter getEraseHandler(JComponent comp) {
		return new MouseAdapter() {

			@Override
			public void mouseExited(MouseEvent e) {
				if (!comp.getVisibleRect().contains(e.getPoint()))
					browser.erase();
			}
			
		};
	}
	
}
