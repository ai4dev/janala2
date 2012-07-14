package tests;

import database.table.*;
import janala.Main;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Map;

public class BookStoreNoSQL {
	// --variables related to control concolic execution--------------------

	private static final int SCREEN_VISIT_MAX = 1;
	private static final int SCREEN_COUNT_MAX = 25;

	private static final int S01_LOGIN_SCREEN = 1;
	private static final int S02_LOGIN_ERROR_SCREEN = 2;
	private static final int S03_MENU_SCREEN = 3;
	private static final int S04_SEARCH_BOOKS_SCREEN = 4;
	private static final int S05_NO_BOOK_SCREEN = 5;
	private static final int S06_SELECT_BOOKS_SCREEN = 6;
	private static final int S07_NO_SELECTION_SCREEN = 7;
	private static final int S08_TOO_EXPENSIVE_SCREEN = 8;
	private static final int S09_FINAL_CHECK_TO_ORDER_SCREEN = 9;
	private static final int S10_THANK_YOU_ORDER_SCREEN = 10;
	private static final int S11_DUPLICATE_ORDER_SCREEN = 11;
	private static final int S20_NO_ORDER_SCREEN = 20;
	private static final int S21_SELECT_ORDERS_SCREEN = 21;
	private static final int S22_NO_SELECTED_ORDER_SCREEN = 22;
	private static final int S23_FINAL_CHECK_TO_CANCEL_SCREEN = 23;
	private static final int S24_FINISHED_CANCEL_SCREEN = 24;

	private int[] screenVisitCountTable;

	private ArrayList<VisitState> visitStateList = new ArrayList<VisitState>();

	// Current inputs
	private BookStoreScreenInputs cis;

	// ---------------------------------------------------------------------------

	// BookStoreScreenInputs screenInputs;

	// --input variables of concolic execution--------------------------------
	// screen inputs
	private BookStoreScreenInputs[] screenInputsList;
	// global inputs
	private int g_customerId;
	// inner variables
	private ArrayList<Tuple2<Integer, Integer>> selectedBookIdPriceList;
	private ArrayList<Tuple2<Integer, Integer>> orderedBookIdPriceList;
	private int[] selectedIndexes;
	private ArrayList<Integer> selectedOrderIdList;
	private Table Customers;
	private Table Orders;
	private Table Publishers;
	private Table Books;
	// local variables for where clause (we can not use clouser...)
	private int l_orderedBookId;

	// ---------------------------------------------------------------------------

	class VisitState {
		public int state;
		public int visitCount;

		public VisitState(int state, int visitCount) {
			this.state = state;
			this.visitCount = visitCount;
		}
	}

	public static void main(String[] argv) {
		try {
			BookStoreNoSQL bookStore = new BookStoreNoSQL();
			bookStore.initialize();
			bookStore.printPreConditions();
			bookStore.execute();
			bookStore.printPostConditions();
			bookStore.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void printPreConditions(){
		System.out.println("--initial database state--");
		PrintDBTables();
	}

	public void printPostConditions(){
		System.out.println("--inputs and updated database state--");
		PrintVisitStatesInputs();
		PrintDBTables();
	}

	private void PrintDBTables(){
		try{
			PrintDBTable(Customers);
			PrintDBTable(Books);
			PrintDBTable(Publishers);
			PrintDBTable(Orders);
		}
		catch(SQLException e){

		}
	}

	private void PrintDBTable(Table table) throws SQLException {
		System.out.println("Table " + table.getName());
		String[] names = table.getColumnNames();
		for (String columnName : names) {
			System.out.print(columnName + " ");
		}
		System.out.println();

		ListIterator<Map<String, Object>> ite = table.iterator();
		while(ite.hasNext()){
			Map<String, Object> record = ite.next();
			for (String columnName : names) {
				System.out.print(record.get(columnName)+" ");
			}
			System.out.println();
		}
		System.out.println();
	}

	private void PrintVisitStatesInputs() {
		System.out.print("visited screens = ");
		for (VisitState visitState : visitStateList) {
			System.out.print(visitState.state +" ");
		}
		System.out.println();

		for (VisitState visitState : visitStateList) {
			System.out.println("Screen " + visitState.state + " (the "
					+ visitState.visitCount + " round)");
			BookStoreScreenInputs is = screenInputsList[visitState.visitCount];
			switch (visitState.state) {
			case S01_LOGIN_SCREEN:
				System.out.println("customerId=" + is.customerId);
				System.out.println("password=" + is.password);
				break;
			case S03_MENU_SCREEN:
				System.out.println("whereGoto=" + is.whereGoto);
				break;
			case S04_SEARCH_BOOKS_SCREEN:
				System.out.println("title=" + is.title);
				System.out.println("publisherName=" + is.publisherName);
				System.out.println("maxYear=" + is.maxYear);
				System.out.println("minYear=" + is.minYear);
				break;
			case S06_SELECT_BOOKS_SCREEN:
				System.out.print("selectedBooksIndexs={ ");
				for (int i = 0; i < selectedIndexes.length; i++) {
					System.out.print(selectedIndexes[i]);
				}
				System.out.println(" }");
				break;
			case S08_TOO_EXPENSIVE_SCREEN:
				System.out.println("isTooExpensiveOk=" + is.isTooExpensiveOk);
				break;
			case S09_FINAL_CHECK_TO_ORDER_SCREEN:
				System.out.println("isOrderOk=" + is.isOderderOK);
				break;
			case S11_DUPLICATE_ORDER_SCREEN:
				System.out.println("isDuplicateOrderOk="
						+ is.isDuplicateOrderOK);
				break;
			}
		}
	}

	public void initialize() {

		screenVisitCountTable = new int[SCREEN_COUNT_MAX];
		screenInputsList = new BookStoreScreenInputs[SCREEN_VISIT_MAX];
		for (int i = 0; i < SCREEN_VISIT_MAX; i++) {
			screenInputsList[i] = new BookStoreScreenInputs();
		}

		try {
			Customers = TableFactory.create("Customers", new String[] { "Id",
					"Name", "PasswordHash", "Age" }, new int[] { Table.INT,
					Table.STRING, Table.INT, Table.INT }, new boolean[] { true,
					false, false, false }, new ForeignKey[] { null, null, null,
					null });

			Orders = TableFactory.create("Orders", new String[] { "Id",
					"CustomerId", "OrderDateTime", "CancelDate", "BookId",
					"IsCanceled" }, new int[] { Table.INT, Table.INT,
					Table.INT, Table.INT, Table.INT, Table.INT },
					new boolean[] { true, false, false, false, false, false },
					new ForeignKey[] { null, new ForeignKey(Customers, "Id"),
							null, null, null, null });

			Publishers = TableFactory.create("Publishers", new String[] { "Id",
					"Name" }, new int[] { Table.INT, Table.STRING },
					new boolean[] { true, false }, new ForeignKey[] { null,
							null });

			Books = TableFactory.create("Books", new String[] { "Id", "ISBN",
					"Title", "Price", "Year", "PublisherId", "Stock" },
					new int[] { Table.INT, Table.INT, Table.STRING, Table.INT,
							Table.INT, Table.INT, Table.INT }, new boolean[] {
							true, false, false, false, false, false, false },
					new ForeignKey[] { null, null, null, null, null,
							new ForeignKey(Publishers, "Id"), null });

			// create an example of inputs and initial database state
			createExampleInitialDatabaseState();
			createExampleInitialInputsList();
			createExampleInitialInnerVariables();

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
	}

	public void createExampleInitialDatabaseState() throws SQLException {

		SymbolicTable.insertSymbolicRows(Customers, 1); // 4

		SymbolicTable.insertSymbolicRows(Orders, 1); // 1*6

		SymbolicTable.insertSymbolicRows(Publishers, 2); // 2*2

		SymbolicTable.insertSymbolicRows(Books, 4); // 4*7
	}

	public void createExampleInitialInputsList() {
		for (int i = 0; i < SCREEN_VISIT_MAX; i++) {
			BookStoreScreenInputs is = screenInputsList[i];
			is.customerId = Main.readInt(0);
			Main.MakeSymbolic(is.customerId);
			is.password = Main.readInt(1);
			Main.MakeSymbolic(is.password);
			is.title = Main.readString("The Art of C#");
			Main.MakeSymbolic(is.title);
			is.publisherName = Main.readString("O Reilly");
			Main.MakeSymbolic(is.publisherName);
			is.minYear = Main.readInt(2000);
			Main.MakeSymbolic(is.minYear);
			is.maxYear = Main.readInt(2010);
			Main.MakeSymbolic(is.maxYear);
		}
	}

	public void createExampleInitialInnerVariables() {
		int selectedIndexSize = 1;
		selectedIndexes = new int[selectedIndexSize];
		for (int i = 0; i < selectedIndexSize; i++) {
			selectedIndexes[i] = Main.readInt(i);
		}
	}

	public void execute() {

		try {
			int state = S01_LOGIN_SCREEN;

			while (true) {

				// check screen visit count
				int screenVisitCount = screenVisitCountTable[state];
				if (screenVisitCount >= SCREEN_VISIT_MAX) {
					System.out.println("Exceed the max count of screen visit");
					return;
				}
				screenVisitCountTable[state]++;
				cis = screenInputsList[screenVisitCount];

				visitStateList.add(new VisitState(state, screenVisitCount));

				// execution the business logic related to state (screen)
				switch (state) {
				case S01_LOGIN_SCREEN:
					state = s01_loginScreen();
					break;
				case S02_LOGIN_ERROR_SCREEN:
					state = s02_loginErrorScreen();
					break;
				case S03_MENU_SCREEN:
					state = s03_menuScreen();
					break;
				case S04_SEARCH_BOOKS_SCREEN:
					state = s04_searchBooksScreen();
					break;
				case S05_NO_BOOK_SCREEN:
					state = s05_noBookScreen();
					break;
				case S06_SELECT_BOOKS_SCREEN:
					state = s06_selectBooksScreen();
					break;
				case S07_NO_SELECTION_SCREEN:
					state = s07_noSelectionScreen();
					break;
				case S08_TOO_EXPENSIVE_SCREEN:
					state = s08_tooExpensiveScreen();
					break;
				case S09_FINAL_CHECK_TO_ORDER_SCREEN:
					state = S09_finalCheckToOrderScreen();
					break;
				case S10_THANK_YOU_ORDER_SCREEN:
					state = S10_thankYouOrderScreen();
					break;
				case S11_DUPLICATE_ORDER_SCREEN:
					state = S11_duplicateOrderScreen();
					break;
				case S20_NO_ORDER_SCREEN:
					state = S20_noOrderScreen();
					break;
				case S21_SELECT_ORDERS_SCREEN:
					state = S21_selectOrdersScreen();
					break;
				case S22_NO_SELECTED_ORDER_SCREEN:
					state = S22_noSelectedOrderScreen();
					break;
				case S23_FINAL_CHECK_TO_CANCEL_SCREEN:
					state = S23_finalCheckToCancelScreen();
					break;
				case S24_FINISHED_CANCEL_SCREEN:
					state = S24_finishedCannelScreen();
					break;
				case -1:
					System.err.println("Invalid state");
					return;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
	}

	public int s01_loginScreen() throws SQLException {

		Main.Assume(cis.customerId >= 0 ? 1 : 0);
		Main.Assume(cis.customerId <= 1000 ? 1 : 0);

		// ResultSet rs =
		// statement.executeQuery("select Id from Customers where Id="+customerId+" and PasswordHash="
		// + hash(password));
		ResultSet rs = Customers.select(new Where() {
			public boolean isTrue(Map<String, Object>[] rows) {
				Integer i = (Integer) rows[0].get("Id");
				if (i == null || i != cis.customerId)
					return false;
				i = (Integer) rows[0].get("PasswordHash");
				if (i == null || i != hash(cis.password)) {
					return false;
				}
				return true;
			}
		}, new String[][] { { "Id" } }, null).getResultSet();

		ArrayList<Integer> customerIdList = new ArrayList<Integer>();
		while (rs.next()) {
			customerIdList.add(rs.getInt("Id"));
		}
		if (customerIdList.size() > 0) {
			g_customerId = cis.customerId;
			return S03_MENU_SCREEN;
		} else {
			return S02_LOGIN_ERROR_SCREEN;
		}
	}

	private int s02_loginErrorScreen() {
		return S01_LOGIN_SCREEN;
	}

	private int s03_menuScreen() throws SQLException {

		Main.Assume((cis.whereGoto == BookStoreScreenInputs.GOTO_ORDER || cis.whereGoto == BookStoreScreenInputs.GOTO_CANCEL) ? 1
				: 0);

		if (cis.whereGoto == BookStoreScreenInputs.GOTO_ORDER) {
			return S04_SEARCH_BOOKS_SCREEN;
		} else if (cis.whereGoto == BookStoreScreenInputs.GOTO_CANCEL) {
			ResultSet rs = Orders.select(new Where() {
				public boolean isTrue(Map<String, Object>[] rows) {
					Integer i = (Integer) rows[0].get("CustomerId");
					if (i == null || i != g_customerId)
						return false;
					return true;
				}
			}, new String[][] { { "Id" } }, null).getResultSet();

			ArrayList<Integer> orderIdList = new ArrayList<Integer>();
			while (rs.next()) {
				orderIdList.add(rs.getInt("Id"));
			}
			if (orderIdList.size() > 0) {
				return S21_SELECT_ORDERS_SCREEN;
			} else {
				return S20_NO_ORDER_SCREEN;
			}
		} else {
			return -1;
		}
	}

	private int s04_searchBooksScreen() throws SQLException {

		Main.Assume(cis.title.length() >= 0 ? 1 : 0);
		Main.Assume(cis.title.length() <= 20 ? 1 : 0);
		Main.Assume(cis.publisherName.length() >= 0 ? 1 : 0);
		Main.Assume(cis.publisherName.length() <= 20 ? 1 : 0);
		Main.Assume(cis.minYear >= 1950 ? 1 : 0);
		Main.Assume(cis.minYear <= 2050 ? 1 : 0);
		Main.Assume(cis.maxYear >= 1950 ? 1 : 0);
		Main.Assume(cis.maxYear <= 2050 ? 1 : 0);

		ResultSet rs = Books.select(new Where() {
			public boolean isTrue(Map<String, Object>[] rows) {
				if (!rows[0].get("PublisherId").equals(rows[1].get("Id")))
					return false;
				if (!cis.title.equals(rows[0].get("Title")))
					return false;
				if ((Integer) rows[0].get("Stock") <= 0)
					return false;
				if ((Integer) rows[0].get("Year") < cis.minYear)
					return false;
				if ((Integer) rows[0].get("Year") > cis.maxYear)
					return false;
				if (!cis.publisherName.equals(rows[1].get("Name")))
					return false;
				return true;
			}
		}, new String[][] { { "Id", "Price" }, null },
				new Table[] { Publishers }).getResultSet();

		// ResultSet rs =
		// statement.executeQuery("select Books.Id, Books.Price from Books inner join Publishers on Books.PublisherId = Publishers.Id "
		// +
		// "where Books.Title= '" + title +
		// "' and Publishers.Name='"+publisherName+"' and Books.Year>=" +
		// minYear +
		// " and Books.Year<=" + maxYear + " and Books.Stock > 0");

		selectedBookIdPriceList = new ArrayList<Tuple2<Integer, Integer>>();
		while (rs.next()) {
			selectedBookIdPriceList.add(new Tuple2<Integer, Integer>(rs
					.getInt("Id"), rs.getInt("Price")));
		}

		if (selectedBookIdPriceList.size() > 0) {
			return S06_SELECT_BOOKS_SCREEN;
		} else {
			return S05_NO_BOOK_SCREEN;
		}
	}

	private int s05_noBookScreen() {//
		return S04_SEARCH_BOOKS_SCREEN;
	}

	private int s06_selectBooksScreen() throws SQLException {
		orderedBookIdPriceList = new ArrayList<Tuple2<Integer,Integer>>();
		for (int selectedIndex : selectedIndexes) {
			orderedBookIdPriceList.add(selectedBookIdPriceList
					.get(selectedIndex));
		}
		int sumPrice = 0;
		for (Tuple2<Integer, Integer> orderedBookIdPrice : orderedBookIdPriceList) {
			sumPrice += orderedBookIdPrice.snd;
		}
		if (sumPrice > 10000) {
			return S08_TOO_EXPENSIVE_SCREEN;
		}

		ResultSet rs = Orders.select(new Where() {
			public boolean isTrue(Map<String, Object>[] rows) {
				Integer i = (Integer) rows[0].get("CustomerId");
				if (i == null || i != g_customerId)
					return false;
				return true;
			}
		}, new String[][] { { "Id" } }, null).getResultSet();

		ArrayList<Integer> alreadyOrderedBookIdList = new ArrayList<Integer>();
		while (rs.next()) {
			alreadyOrderedBookIdList.add(rs.getInt("Id"));
		}

		int dupulicateOrderCount = 0;
		for (Tuple2<Integer, Integer> orderedBookIdPrice : orderedBookIdPriceList) {
			if (alreadyOrderedBookIdList.contains(orderedBookIdPrice.fst)) {
				dupulicateOrderCount++;
			}
		}

		if (dupulicateOrderCount > 0) {
			return S11_DUPLICATE_ORDER_SCREEN;
		}

		return S09_FINAL_CHECK_TO_ORDER_SCREEN;
	}

	private int s07_noSelectionScreen() {
		return S06_SELECT_BOOKS_SCREEN;
	}

	private int s08_tooExpensiveScreen() {
		if (cis.isTooExpensiveOk) {
			return S09_FINAL_CHECK_TO_ORDER_SCREEN;
		} else {
			return S06_SELECT_BOOKS_SCREEN;
		}
	}

	private int S09_finalCheckToOrderScreen() {

		final int CURREMT_TIME_STAMP = 20120714;

		for (Tuple2<Integer, Integer> orderedBookIdPrice : orderedBookIdPriceList) {
			Object[] newOrder = { g_customerId, null, CURREMT_TIME_STAMP, null,
					orderedBookIdPrice.fst, 0 };
			Orders.insert(newOrder);

			l_orderedBookId = orderedBookIdPrice.fst;

			Orders.update(new Where() {
				public boolean modify(Map<String, Object>[] rows) {
					Integer id = (Integer) rows[0].get("Id");
					if (id == null || id != l_orderedBookId) {
						return false;
					} else {
						Integer stock = (Integer) rows[0].get("Stock");
						rows[0].put("Stock", stock + 1);
						return true;
					}
				}

				public boolean isTrue(Map<String, Object>[] rows) {
					return true;
				}
			});
		}

		return S10_THANK_YOU_ORDER_SCREEN;
	}

	private int S10_thankYouOrderScreen() {
		return S04_SEARCH_BOOKS_SCREEN;
	}

	private int S11_duplicateOrderScreen() {
		if (cis.isDuplicateOrderOK) {
			return S09_FINAL_CHECK_TO_ORDER_SCREEN;
		} else {
			return S06_SELECT_BOOKS_SCREEN;
		}
	}

	private int S20_noOrderScreen() {
		return 0;
	}

	private int S21_selectOrdersScreen() {
		return 0;
	}

	private int S22_noSelectedOrderScreen() {
		return 0;
	}

	private int S23_finalCheckToCancelScreen() {
		return 0;
	}

	private int S24_finishedCannelScreen() {
		return 0;
	}

	private int hash(int password) {

		return password % 5 + 3;
	}

	public void dispose() {

	}

}
