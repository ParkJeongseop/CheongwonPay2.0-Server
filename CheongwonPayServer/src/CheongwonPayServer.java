import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class CheongwonPayServer {
	public static final int port = 923;// ���� ��Ʈ�� �������־���.
	public static ArrayList<User> userList;// ���� Ŭ���̾�Ʈ ����̽��� �迭�� �����Ѵ�.
	public static Connection con = null;
	public static java.util.Date mTime, wTime;

	public static void main(String[] args) throws IOException {
		ServerSocket server;
		Socket newClient;
		User newUser;
		userList = new ArrayList<User>();
		server = new ServerSocket(port);

		try {// ���ŷ�Ž���ý���(FDS)���� �̿��� ����, ���� ���� ���۽ð��� DateŸ������ ��ȯ�Ͽ� �����Ѵ�.
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
			String strDate = /*"2018-09-07 09:00"*/"2018-08-18 22:57";
			wTime = dateFormat.parse(strDate);
			strDate = /*"2018-09-07 12:00"*/"2018-08-18 22:57";
			mTime = dateFormat.parse(strDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql://localhost/cheongwonpaydb?useUnicode=true&characterEncoding=utf-8", "testuser", "testuserpassword");// DB����(MySQL)�ּҿ�
																														// ����,
																														// �н�����
			// DB����
		} catch (SQLException sqex) {
			System.out.println("SQLException: " + sqex.getMessage());
			System.out.println("SQLState: " + sqex.getSQLState());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		while (true) {
			System.out.println("Waiting for client...");
			newClient = server.accept();
			newUser = new User("unknown", newClient);
			newUser.start();// ���� ���ӽ� ������ ArrayList�� �߰�
			userList.add(newUser);
		}
	}
	// ���� ���� �� ���Ʈ ��
}

class User extends Thread {

	public static final int OP_LOGIN = 1, OP_PURCHASE = 2, OP_ADD_ITEM = 3, OP_RF_BAL = 4, OP_GET_GOODS_LIST = 5,
			OP_GET_REFUND_LIST = 6, OP_REFUND = 7, OP_ATD = 10, OP_EDIT_GOODS = 11, OP_DELETE_GOODS = 12,
			OP_EDIT_PW = 13, OP_GET_NAME = 14, OP_USER_MATCHING = 15, OP_CHARGE = 16, OP_CHANGEINFO = 17,
			OP_REFUND_RS_ALREADY_REFUNDED = 101, OP_PURCHASE_RS_NOTIME = 103, OP_PURCHASE_RS_OVERLIMIT = 104,
			OP_PURCHASE_RS_USERNULL = 106, OP_PURCHASE_RS_SUCCESS = 105, OP_EXIT = 1110;// ��Ž�
														// �̿��ϴ�
														// ���
														// �ڵ�(OP-Code)��
														// ����
														// ������Ÿ������ �����Ѵ�.
	public static final String OP_GET_GOODS_LIST_FIN = "##";// ��Ž� �̿��ϴ� ��� �ڵ�(OP-Code)�� ���� ������Ÿ������ �����Ѵ�. �� �ڵ常 ����������
															// ����ϴ�
															// ������ �Ʒ����� DataInputStream�� �� ���������� �ҷ����� �����̴�

	String userName;
	Socket socket;

	DataInputStream dis;
	DataOutputStream dos;

	public User(String s, Socket sc) {
		userName = s;
		socket = sc;
	}

	@Override
	public void run() {

		try {

			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());

		} catch (IOException ex) {
			Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
			CheongwonPayServer.userList.remove(this);
			return;
		}

		// String readData = null;// ���� �����͸� ���� ������Ÿ������ �����Ѵ�. -> �����丵 ���� �����
		int readOPData = 0;// ���� OP-Code�� ���� �迭�� �����Ѵ�.

		System.out.println("Socket Opened!");
		CheongwonPayDB dbHandler = new CheongwonPayDB(userName);

		while (true) {
			try {
				readOPData = dis.readInt();
				System.out.print("readOPData : " + readOPData);
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}

			if (readOPData == OP_LOGIN) {// �α���
				System.out.println(" : OP_LOGIN");
				try {

					String Club_Name = null;
					String data = dis.readUTF();
					boolean isSuccess = dbHandler.login(data);
					Club_Name = dbHandler.get_club_name(data);
					if (isSuccess) {
						dos.writeUTF("Login Success!");
						dos.writeUTF(Club_Name);
					} else {
						dos.writeUTF("Login Failed!");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (readOPData == OP_PURCHASE) {// �ŷ�, �⼮üũ
				System.out.println(" : OP_PURCHASE");
				try {
					int result = dbHandler.purchase(dis.readUTF());
					if (result != -1) {
						dos.writeInt(result);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (readOPData == OP_ADD_ITEM) {// ��ǰ�߰�
				System.out.println(" : OP_ADD_ITEM");
				try {
					dbHandler.addItem(dis.readUTF());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (readOPData == OP_RF_BAL) {// �ܾ�, �⼮�� ��ȸ + User����Ʈ�� �������(����)�߰�
				System.out.println(" : OP_RF_BAL");
				try {
					String result = dbHandler.getBalanceVisits(dis.readUTF());
					if (result != null) {
						dos.writeUTF(result);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (readOPData == OP_GET_GOODS_LIST) {// ��ǰ��Ϻҷ����� goodsnum,name,price�ֱ�
				System.out.println(" : OP_GET_GOODS_LIST");
				try {
					String[] goodsList = dbHandler.getGoodsList();
					for (String nextGood : goodsList) {
						dos.writeUTF(nextGood);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (readOPData == OP_GET_REFUND_LIST) {// get����ڰ������ for refund time,goodsnum,name,price�ֱ�
				System.out.println(" : OP_GET_REFUND_LIST");
				try {
					String[] refundList = dbHandler.getRefundList(dis.readUTF());
					for (String nextList : refundList) {
						dos.writeUTF(nextList);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			if (readOPData == OP_REFUND) {// ��ü��� (+������)
				System.out.println(" : OP_REFUND");
				try {
					int result = dbHandler.refund(dis.readUTF());
					if (result != 0 && result != -1) {
						dos.writeInt(result);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (readOPData == OP_ATD) {// �⼮üũ �ο����
				System.out.println(" : OP_ATD");
				try {
					dbHandler.attendance(dis.readUTF());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (readOPData == OP_EDIT_GOODS) {// ��ǰ��Ϻ���
				System.out.println(" : OP_EDIT_GOODS");
				try {
					dbHandler.edit_goods(dis.readUTF());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (readOPData == OP_DELETE_GOODS) {// ��ǰ����
				System.out.println(" : OP_DELETE_GOODS");
				try {
					dbHandler.delete_goods(dis.readUTF());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (readOPData == OP_EDIT_PW) {// �н����� ����
				System.out.println(" : OP_EDIT_PW");
				try {
					dbHandler.edit_password(dis.readUTF());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (readOPData == OP_GET_NAME) {// �̸���ȸ
				System.out.println(" : OP_GET_NAME");
				try {
					String result = dbHandler.get_username(dis.readUTF());
					dos.writeUTF(result);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			/*
			 * if (readOPData == OP_USER_MATCHING) {// ������Ī try { readData =
			 * dis.readUTF();// Ŭ���̾�Ʈ�� ���� ���ڵ�, �й�, �б����� ���� "readData"�� �����Ѵ�. // ���� ������ ��������
			 * ó���� �� �ֵ��� ���� �и��Ͽ� �����Ѵ�. String Barcode = readData.split(":")[0]; String
			 * Student_ID = readData.split(":")[1]; String Type = readData.split(":")[2];
			 * 
			 * String Grade = Student_ID.substring(0, 1); String Class =
			 * Student_ID.substring(1, 3); String Number = Student_ID.substring(3);
			 * System.out.println("User : " + Barcode); System.out.println("Student_ID : " +
			 * Grade + ":" + Class + ":" + Number);
			 * 
			 * java.sql.Statement st = null; st =
			 * CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
			 * ResultSet.CONCUR_READ_ONLY);
			 * 
			 * switch (Type) { case "1":// ������ �� st.execute("UPDATE user set User='" +
			 * Barcode + "' where Grade='" + Grade + "' and Class='" + Class +
			 * "' and Number='" + Number + "' and School='M'"); break; case "2":// ������ ��
			 * st.execute("UPDATE user set User='" + Barcode + "' where Grade='" + Grade +
			 * "' and Class='" + Class + "' and Number='" + Number + "' and School='W'");
			 * break; } st.close(); } catch (IOException e) { e.printStackTrace(); } catch
			 * (SQLException ex) { Logger.getLogger(User.class.getName()).log(Level.SEVERE,
			 * null, ex); } }
			 */

			if (readOPData == OP_CHARGE) {
				System.out.println(" : OP_CHARGE");
				try {
					dbHandler.charge(dis.readUTF());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (readOPData == OP_CHANGEINFO) {
				System.out.println(" : OP_CHANGEINFO");
				try {
					dbHandler.change_info(dis.readUTF());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (readOPData == OP_EXIT) {// exit
				System.out.println(" : OP_EXIT");
				break;
			}

		} // ������ ���� �κ�
		System.out.println("thread closed");
		try {
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			dis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		CheongwonPayServer.userList.remove(this);
	}

}

class CheongwonPayDB {
	String userName;

	int Club_Num;
	String School_Type;

	public CheongwonPayDB(String u) {
		userName = u;
	}

	boolean simplifiedFDS(String School_Type) {// ���ŷ�Ž���ý���(FDS) �����ð��� ��� true, �����ð��̿��� ��� false�� ��ȯ.
		// ����ð� ����
		long now = System.currentTimeMillis();
		java.util.Date currentTime = new java.util.Date(now);

		// �����ð��� �ŷ����� Ȯ��
		switch (School_Type) {
		case "M":// ������ ��
			if (currentTime.compareTo(CheongwonPayServer.mTime) < 0) {
				return false;
			}
			break;
		case "W":// ������ ��
			if (currentTime.compareTo(CheongwonPayServer.wTime) < 0) {
				return false;
			}
			break;
		case "U":// ���յ��Ƹ��� ��
			if (currentTime.compareTo(CheongwonPayServer.wTime) < 0) {
				return false;
			}
			break;
		}
		return true;
	}

	boolean login(String readData) {
		boolean toReturn = false;
		try {// �α��ν� DataInputStream ������ ID:PW �����̴�.
			System.out.println("ID:PW : " + readData);
			// ���� ������ �������� ó���� �� �ֵ��� ���� �и��Ͽ� �����Ѵ�.
			String ID = readData.split(":")[0];
			String PW = readData.split(":")[1];

			String club_name;
			java.sql.PreparedStatement st = null;
			ResultSet rs = null;
			st = CheongwonPayServer.con.prepareStatement("SELECT * FROM club WHERE Name=?");
			st.setString(1, ID);

			if (st.execute()) {// "ID"�� ��ġ�ϴ� �����͸� �ҷ�����.
				rs = st.getResultSet();
			}

			String password = null;// "password"�� ���� �н�����, "Club_Name"�� ���Ƹ����� ���� ������Ÿ������ �����Ѵ�.

			while (rs.next()) {
				// ���� �н����� �ҷ�����
				password = rs.getString("PW");
				System.out.println("realPW : " + password);

				// ���Ƹ� �̸� �ҷ�����
				club_name = rs.getString("Name");
				System.out.println("Club_Name : " + club_name);

				// ���Ƹ�������ȣ����
				Club_Num = rs.getInt("Club_Num");
				System.out.println("Club_Num : " + Club_Num);

				// ���Ƹ� �Ҽ� �б����� ����
				School_Type = rs.getString("School");
				System.out.println("School_Type : " + School_Type);
			}

			if (password != null && password.equals(PW)) {// �α��μ���������
				System.out.println("Login Success!");
				toReturn = true;

			} else {// �α��� ����
				System.out.println("Login Failed!");
				System.out.println("db PW : " + password + " .");
				System.out.println("PW    : " + PW + " .");
			}

			st.close();

		} catch (SQLException sqex) {// SQL��� �� ������ ������ �� �α� ���
			System.out.println("SQLException: " + sqex.getMessage());
			System.out.println("SQLState: " + sqex.getSQLState());
		}
		return toReturn;
	}

	String get_club_name(String data) {
		String club_name = null;
		String readData = data;
		String id = readData.split(":")[0];

		club_name = id;
		return club_name;

	}

	int purchase(String data) {
		String student = data.split(":")[0]; // ���ڵ� ������
		int Goods_Num = Integer.parseInt(data.split(":")[1]);
		System.out.println("User : " + student);
		System.out.println("Goods_Num : " + Goods_Num);
		int toReturn;

		int Balance = 0, Price = 0;// "Balance"�� �ܾ�, "Price"�� ��ǰ���� ���� ������Ÿ������ �����Ѵ�.

		if (student.equals("null")) {// ���ڵ嵥���Ͱ� null�� ��
			toReturn = User.OP_PURCHASE_RS_USERNULL;
		} else {
			try {
				if (simplifiedFDS(School_Type)) {// �����ð��� ��
					java.sql.Statement st = null;
					ResultSet rs = null;
					st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
							ResultSet.CONCUR_READ_ONLY);

					// �ܾ���ȸ
					if (st.execute("SELECT Balance FROM user WHERE User='" + student + "'")) {
						rs = st.getResultSet();
					}
					// �ܾ� �߰� if�� �Է�

					while (rs.next()) {
						Balance = rs.getInt(1);
						System.out.println("Balance : " + Balance);
					}

					// ��ǰ������ȸ
					if (st.execute("SELECT Price FROM goods WHERE Goods_Num='" + Goods_Num + "'")) {
						rs = st.getResultSet();
					}
					while (rs.next()) {
						Price = rs.getInt(1);
						System.out.println("Price : " + Price);
					}

					// �ŷ��������� �ܾ�>=��ǰ��
					// ������� �ּ�ȭ�� ���� ���.
					if (Balance >= Price) {
						// �ŷ������߰�
						st.execute("INSERT INTO transactions (User, Club_Num, Goods_Num, Price) VALUES ('" + student + "',"
								+ Club_Num + "," + Goods_Num + "," + Price + ")");

						// Balance����
						st.execute("UPDATE user SET Balance=(Balance-" + Price + ") WHERE User='" + student + "'");

						// ���Ƹ� ��������
						st.execute("UPDATE club SET Income=(Income+" + Price + ") WHERE Club_Num='" + Club_Num + "'");

						//// �⼮üũ
						// ���ŷ�Ž���ý���(FDS) 10���̳��� �ߺ� �⼮üũ ����
						/*String LastCheckTime = null;
						java.util.Date LastCheckT = null;

						if (st.execute("SELECT Time FROM atd_history WHERE User='" + student + "'")) {
							rs = st.getResultSet();
						}
						while (rs.next()) {
							// ������ �⼮üũ �� �ð� ���� (���� ������ �������� �� ������ �ֱ⶧���� ���������δ� ������ �⼮�ð��� ����ȴ�.)
							LastCheckTime = rs.getString("Time");
						}

						// ������ �⼮üũ �� �ð��� String to Date
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
								java.util.Locale.getDefault());
						try {
							LastCheckT = dateFormat.parse(LastCheckTime);
						} catch (ParseException e) {
							e.printStackTrace();
						}

						// LastCheckT�� +10���ϱ�
						Calendar cal = Calendar.getInstance();
						cal.setTime(LastCheckT);
						cal.add(Calendar.MINUTE, 10);
						LastCheckT = cal.getTime();

						// ����ð� ����
						long now = System.currentTimeMillis();
						java.util.Date currentTime = new java.util.Date(now);

						if (currentTime.compareTo(LastCheckT) > 0) {// ������ �⼮üũ �� �ð� +10�� ���� ����ð��� �� Ŭ ��
							if (st.execute("SELECT Time FROM atd_history WHERE User='" + student + "'and Club_Num ="
									+ Club_Num + "")) {// �� ���Ƹ������� �л��� ������� �ҷ�����
								rs = st.getResultSet();
							}

							boolean isChecked = false;

							while (rs.next()) {
								isChecked = true;
							}
							if (!isChecked) {// �� ���Ƹ����� �� �л��� �⼮üũ ������ ���� �� (�ʿ伺 �ǹ�?)
								st.execute("INSERT INTO atd_history (User, Club_Num) VALUES ('" + student + "', "
										+ Club_Num + ")");
								st.execute("UPDATE user SET Visits=(Visits+1) WHERE User='" + student + "'");
								st.execute("UPDATE club SET Visits=(Visits+1) WHERE Club_Num='" + Club_Num + "'");// �ʿ伺
																													// �ǹ�?
							} else {// �������Ƹ����� 2ȸ�̻� �⼮üũ �õ�

							}
						} else {// 10���̳��� ������ �⼮üũ �õ�

						}*/
						// �ŷ�����
						toReturn = User.OP_PURCHASE_RS_SUCCESS;
						// �ŷ�X ���� �κ�
					} else {
						// �ŷ����� �ܾ׺���
						toReturn = User.OP_PURCHASE_RS_OVERLIMIT;
					}
					st.close();
				} else {// �����ð� �̿ܿ� ��û
					toReturn = User.OP_PURCHASE_RS_NOTIME;
				}
			} catch (SQLException e) {
				Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, e);
				toReturn = -1;
			}
		}
		return toReturn;
	}

	void addItem(String readData) {
		try {
			System.out.println("Goods_Name, Price : " + readData);

			if (!simplifiedFDS(School_Type)) {// �����ð� �̿��� ��
				// ���� ������ �������� ó���� �� �ֵ��� ���� �и��Ͽ� �����Ѵ�.
				String Goods_Name = readData.split(":")[0];
				int Price = Integer.parseInt(readData.split(":")[1]);

				java.sql.Statement st = null;
				st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY);
				st.execute("INSERT INTO goods (Club_Num, Goods_Name, Price) VALUES ('" + Club_Num + "','" + Goods_Name
						+ "','" + Price + "')");// �����ͺ��̽��� ���Ƹ�������ȣ, ��ǰ��, ��ǰ���� �����͸� �����Ѵ�.
				st.close();
			}
		} catch (SQLException ex) {
			Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	String getBalanceVisits(String readData) {
		String toReturn = null;
		try {
			System.out.println("User : " + readData);

			java.sql.Statement st = null;
			ResultSet rs = null;
			st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

			if (st.execute("SELECT * FROM user WHERE User='" + readData + "'")) {// ���ڵ忡 ��ġ�ϴ� �л������� �ҷ�����
				rs = st.getResultSet();
			}
			// ����̵�Ͻ� X
			if (!rs.next()) {// �ҷ��� �л������Ͱ� ���� ��
				// User����Ʈ�� �߰�
				rs.close();
				st.execute("INSERT INTO user (User) VALUES ('" + readData + "')");
				if (st.execute("SELECT * FROM user WHERE User='" + readData + "'")) {
					rs = st.getResultSet();
				}
			}

			rs.beforeFirst();

			while (rs.next()) {
				String str = rs.getInt("Balance") + ":" + rs.getInt("Visits") + ":";

				switch (rs.getString("School")) {// ����,������ �⼮üũ������ �ٸ��⶧���� switch�� ���
				case "M":// ������ ��
					if (rs.getInt("Club_Num") != 0 || rs.getInt("Visits") >= 3) {// �ο������ �Ǿ��ְų�, �ν��̿�Ƚ���� 3ȸ�̻��� ��
						str += "1";
					} else {
						str += "0";
					}
					break;
				case "W":// ������ ��
					if (rs.getInt("Visits") >= 5) {// �ν��̿�Ƚ���� 5ȸ�̻��� ��
						str += "1";
					} else {
						str += "0";
					}
					break;
				default:
					str += "1";
					break;
				}
				System.out.println("Result : " + str);
				toReturn = str;

			}
			st.close();
		} catch (SQLException ex) {
			Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
		}
		return toReturn;
	}

	String[] getGoodsList() {
		java.sql.Statement st = null;
		ResultSet rs = null;
		ArrayList<String> toReturn = new ArrayList<>();

		try {
			st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

			if (st.execute("SELECT * FROM goods WHERE Club_Num='" + Club_Num + "'")) {// ���Ƹ�������ȣ�� ��ġ�ϴ� ��ǰ�� �ҷ�����
				rs = st.getResultSet();
			}
			while (rs.next()) {
				toReturn.add(rs.getInt("Goods_Num") + ":" + rs.getString("Goods_Name") + ":" + rs.getInt("Price"));// ��ǰ������ȣ,
																													// ��ǰ��,
																													// ��ǰ����
																													// ����
			}
			toReturn.add(User.OP_GET_GOODS_LIST_FIN);// ��ǰ��� ��������OP����

			st.close();
		} catch (SQLException ex) {
			Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
		}
		System.out.println(toReturn);
		return toReturn.toArray(new String[toReturn.size()]);
	}

	String[] getRefundList(String readData) {
		java.sql.Statement st = null;
		ResultSet rs = null;
		java.sql.Statement st2 = null;
		ResultSet rs2 = null;
		ArrayList<String> toReturn = new ArrayList<>();
		try {

			st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			st2 = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

			if (st.execute(
					"SELECT * FROM transactions WHERE Club_Num=" + Club_Num + " and User='" + readData + "'")) {
				rs = st.getResultSet();
			}

			while (rs.next()) {
				int goods_num = rs.getInt("Goods_Num");
				st2.execute("SELECE Goods_Name FROM goods WHERE Goods_Num=" + goods_num);
				rs2 = st2.getResultSet();
				rs2.next();
				
				toReturn.add(rs.getString("Time") + ":" + rs.getString("Goods_Num") + ":" + rs2.getString(1));
			}
			toReturn.add(User.OP_GET_GOODS_LIST_FIN);// ��ǰ�����������OP

			st.close();
			st2.close();
		} catch (SQLException ex) {
			Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
		}
		return toReturn.toArray(new String[toReturn.size()]);
	}

	int refund(String readData) {
		int toReturn = 0;

		try {
			java.sql.Statement st = null;
			ResultSet rs = null;
			st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = st.executeQuery("SHOW DATABASES");

			String temp[] = { null, null, null };// ����ҿ���:���ڵ�:��ǰ��

			// �̹���ҵȰ������� Ȯ��
			if (st.execute("SELECT Cancel FROM transactions WHERE Num='" + readData + "'")) {
				rs = st.getResultSet();
			}
			while (rs.next()) {
				temp[0] = rs.getString(1);// ���� SQL������ NCHAR���� �������� �ʾƼ� �ٲ�.
				System.out.println(temp[0]);
			}

			if (temp[0] == null) {
				// Num������ user���ڵ�Ȯ��
				if (st.execute("SELECT User FROM transactions WHERE Num='" + readData + "'")) {
					rs = st.getResultSet();
				}
				while (rs.next()) {
					temp[1] = rs.getString(1);
					System.out.println(temp[1]);
				}

				// Num������ ����Ȯ��
				if (st.execute("SELECT Price FROM transactions WHERE Num='" + readData + "'")) {
					rs = st.getResultSet();
				}
				while (rs.next()) {
					temp[2] = rs.getString(1);
					System.out.println(temp[2]);
				}

				// �ŷ����ǥ��
				if (st.execute("UPDATE transactions SET Cancel = '1' WHERE Num='" + readData + "'"))
					;

				// Balance ����
				if (st.execute("UPDATE user SET Balance = (Balance-" + temp[2] + " WHERE User='" + temp[1] + "'"))
					;

				// ���Ƹ� Income ����
				if (st.execute("UPDATE club SET Income = (Income-" + temp[2] + " WHERE Club_Num='" + Club_Num + "'"))
					;

				st.close();
			} else {
				toReturn = User.OP_REFUND_RS_ALREADY_REFUNDED;// �̹� ��ҵ� ����
			}
		} catch (SQLException ex) {
			Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
			toReturn = -1;
		}
		return toReturn;
	}

	void attendance(String readData) {
		try {
			java.sql.Statement st = null;
			st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			System.out.println("OP_ATD : " + readData);
			st.execute("UPDATE user SET Club_Num='" + Club_Num + "' WHERE User='" + readData + "'");// ���ڵ忡
																									// ��ġ�ϴ�
																									// �л���
																									// ���Ƹ�
																									// ������ȣ�����ϱ�
			st.close();
		} catch (SQLException ex) {
			Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	void edit_goods(String readData) {

		if (!simplifiedFDS(School_Type)) {// �����ð� �̿��� ��
			try {
				int Goods_Num = Integer.parseInt(readData.split(":")[0]);
				String Goods_Name = readData.split(":")[1];
				int Price = Integer.parseInt(readData.split(":")[2]);

				java.sql.Statement st = null;
				st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY);

				System.out.println(readData);

				st.execute("UPDATE goods SET Goods_Name='" + Goods_Name + "', Price='" + Price + "' WHERE Goods_Num='"
						+ Goods_Num + "'");
				st.close();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (SQLException ex) {
				Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	void delete_goods(String readData) {
		try {
			System.out.println(readData);

			if (!simplifiedFDS(School_Type)) {// �����ð� �̿��� ��
				java.sql.Statement st = null;
				st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
						ResultSet.CONCUR_READ_ONLY);

				st.execute("DELETE From goods WHERE Goods_Num='" + readData + "'");
				st.close();
			}
		} catch (SQLException ex) {
			Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	void edit_password(String readData) {
		try {
			System.out.println("after pw: " + readData);

			java.sql.Statement st = null;
			st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

			st.execute("UPDATE club SET PW='" + readData + "' WHERE Club_Num='" + Club_Num + "'");// �н����带
																									// �����Ѵ�.
			st.close();
		} catch (SQLException ex) {
			Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	String get_username(String readData) {
		String toReturn = null;
		try {
			System.out.println("User : " + readData);

			java.sql.Statement st = null;
			ResultSet rs = null;
			st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

			if (st.execute("SELECT Name FROM user WHERE User='" + readData + "'")) {// ���ڵ忡 ��ġ�ϴ� �л��� �ҷ�����
				rs = st.getResultSet();
			}

			while (rs.next()) {
				String str = rs.getNString(1);
				if (str != null) {// null�� �ƴ� ��
					toReturn = str;// �̸����� �����Ѵ�.
				}
			}

			st.close();
		} catch (SQLException ex) {
			Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
		}
		if (toReturn == null) {
			toReturn = "lookup error!";
		}
		return toReturn;
	}

	void charge(String readData) {
		try {
			String user = readData.split(":")[0];
			String wtbalance = readData.split(":")[1];// wtbalance�� ������ ����.

			java.sql.Statement st = null;
			st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

			System.out.println("User : " + user);
			System.out.println("Balance : " + wtbalance);

			if (user.equals("null")) {// ���ڵ嵥���Ͱ� null�� ��
				return;
			} else {// ��������
				st.execute("UPDATE user SET Balance=(Balance+" + wtbalance + ") WHERE User='" + user + "'");
			}
			st.close();
		} catch (SQLException ex) {
			Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	void change_info(String readData) {
		try {
			String User = readData.split(":")[0];
			String newName = readData.split(":")[1];
			System.out.println("name = " + newName);
			String newSchool = readData.split(":")[2];
			int newGrade = Integer.parseInt(readData.split(":")[3]);
			int newClass = Integer.parseInt(readData.split(":")[4]);
			int newNumber = Integer.parseInt(readData.split(":")[5]);

			java.sql.Statement st = null;
			st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

			st.execute("UPDATE user SET Name='" + newName + "', School='" + newSchool + "',Grade=" + newGrade
					+ ",Class=" + newClass + ",Number=" + newNumber + " WHERE User='" + User + "'");

			st.close();
		} catch (SQLException ex) {
			Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}

//TODO �⼮üũ �ý����� ������ ���������ٸ�?