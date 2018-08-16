import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
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
			String strDate = "2018-09-07 09:00";
			wTime = dateFormat.parse(strDate);
			strDate = "2018-09-07 12:00";
			mTime = dateFormat.parse(strDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection("jdbc:mysql://localhost/cheongwonpaydb", "testuser", "testuserpassword");// DB����(MySQL)�ּҿ�
																														// ����,
																														// �н�����
			// DB����
		} catch (SQLException sqex) {
			System.out.println("SQLException: " + sqex.getMessage());
			System.out.println("SQLState: " + sqex.getSQLState());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		java.sql.Statement str = null;

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
			OP_PURCHASE_RS_USERNULL = 106, OP_PURCHASE_RS_SUCCESS = 105, OP_CHARGE_RS_USERNULL = 107,
			OP_CHARGE_RS_SUCCESS = 108, OP_EXIT = 1110;// ��Ž�
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

		String readData = null;// ���� �����͸� ���� ������Ÿ������ �����Ѵ�.
		int readOPData = 0;// ���� OP-Code�� ���� �迭�� �����Ѵ�.

		int Club_Num = 0;// ���Ƹ� ������ȣ�� ���� �迭�� �����Ѵ�.
		String School_Type = null;// �б�����(����� M, ����� W, ���յ��Ƹ��� U)�� ���� ������Ÿ������ �����Ѵ�.
		System.out.println("Socket Opened!");
		CheongwonPayDB dbHandler = new CheongwonPayDB(userName);

		while (true) {
			try {
				readOPData = dis.readInt();
				System.out.println("readOPData : " + readOPData);
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}

			if (readOPData == OP_LOGIN) {// �α���
				String Club_Name = null;
				try {
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
				try {
					dbHandler.addItem(dis.readUTF());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (readOPData == OP_RF_BAL) {// �ܾ�, �⼮�� ��ȸ + User����Ʈ�� �������(����)�߰�
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
				try {
					dbHandler.attendance(dis.readUTF());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (readOPData == OP_EDIT_GOODS) {// ��ǰ��Ϻ���
				try {
					dbHandler.edit_goods(dis.readUTF());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (readOPData == OP_DELETE_GOODS) {// ��ǰ����
				try {
					readData = dis.readUTF();// Ŭ���̾�Ʈ�� ���� ��ǰ������ȣ�� "readData"�� �����Ѵ�.
					System.out.println(readData);

					if (!simplifiedFDS(School_Type)) {// �����ð� �̿��� ��
						java.sql.Statement st = null;
						st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
								ResultSet.CONCUR_READ_ONLY);

						st.execute("DELETE From goods where Goods_Num='" + readData + "'");
						st.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (SQLException ex) {
					Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
				}
			}

			if (readOPData == OP_EDIT_PW) {// �н����� ����
				try {
					readData = dis.readUTF();// Ŭ���̾�Ʈ�� ���� ������ �н����带 "readData"�� �����Ѵ�.
					System.out.println("after pw: " + readData);

					java.sql.Statement st = null;
					st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
							ResultSet.CONCUR_READ_ONLY);

					st.execute("UPDATE club set PW='" + readData + "' where Club_Num='" + Club_Num + "'");// �н����带
																											// �����Ѵ�.
					st.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (SQLException ex) {
					Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
				}
			}

			if (readOPData == OP_GET_NAME) {// �̸���ȸ
				try {
					readData = dis.readUTF();// Ŭ���̾�Ʈ�� ���� ���ڵ��� "readData"�� �����Ѵ�.
					System.out.println("User : " + readData);

					java.sql.Statement st = null;
					ResultSet rs = null;
					st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
							ResultSet.CONCUR_READ_ONLY);

					if (st.execute("SELECT Name FROM user where User='" + readData + "'")) {// ���ڵ忡 ��ġ�ϴ� �л��� �ҷ�����
						rs = st.getResultSet();
					}

					while (rs.next()) {
						String str = rs.getNString(1);
						if (str != null) {// null�� �ƴ� ��
							dos.writeUTF(str);// �̸����� �����Ѵ�.
						} else {
							dos.writeUTF("lookup error!");// "lookup error!"�̶�� �����Ѵ�.
						}
					}

					st.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (SQLException ex) {
					Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
				}
			}

			/*if (readOPData == OP_USER_MATCHING) {// ������Ī
				try {
					readData = dis.readUTF();// Ŭ���̾�Ʈ�� ���� ���ڵ�, �й�, �б����� ���� "readData"�� �����Ѵ�.
					// ���� ������ �������� ó���� �� �ֵ��� ���� �и��Ͽ� �����Ѵ�.
					String Barcode = readData.split(":")[0];
					String Student_ID = readData.split(":")[1];
					String Type = readData.split(":")[2];
					
					String Grade = Student_ID.substring(0, 1);
					String Class = Student_ID.substring(1, 3);
					String Number = Student_ID.substring(3);
					System.out.println("User : " + Barcode);
					System.out.println("Student_ID : " + Grade + ":" + Class + ":" + Number);

					java.sql.Statement st = null;
					st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
							ResultSet.CONCUR_READ_ONLY);

					switch (Type) {
					case "1":// ������ ��
						st.execute("UPDATE user set User='" + Barcode + "' where Grade='" + Grade + "' and Class='"
								+ Class + "' and Number='" + Number + "' and School='M'");
						break;
					case "2":// ������ ��
						st.execute("UPDATE user set User='" + Barcode + "' where Grade='" + Grade + "' and Class='"
								+ Class + "' and Number='" + Number + "' and School='W'");
						break;
					}
					st.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (SQLException ex) {
					Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
				}
			}*/

			if (readOPData == OP_CHARGE) {
				// �ܾ� ������ �ڵ�
				// user:wtbalance�������� dis;
				try {
					readData = dis.readUTF();

					String User = readData.split(":")[0];
					String wtbalance = readData.split(":")[1];// wtbalance�� ������ ����.

					java.sql.Statement st = null;
					st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
							ResultSet.CONCUR_READ_ONLY);

					System.out.println("User : " + User);
					System.out.println("Balance : " + wtbalance);

					if (User.equals("null")) {// ���ڵ嵥���Ͱ� null�� ��
						dos.writeInt(OP_CHARGE_RS_USERNULL);
					} else {// ��������
						st.execute("UPDATE user SET Balance=(Balance+" + wtbalance + ") where User='" + User + "'");
						dos.writeInt(OP_CHARGE_RS_SUCCESS);
					}
					st.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (SQLException ex) {
					Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
				}
			}

			
			  if (readOPData == OP_CHANGEINFO) {
					  try {
						readData = dis.readUTF();
  
						String User = readData.split(":")[0];
						String newName = readData.split(":")[1];
						String newSchool = readData.split(":")[2];
						int newGrade = Integer.parseInt(readData.split(":")[3]);
						int newClass = Integer.parseInt(readData.split(":")[4]);
						int newNumber = Integer.parseInt(readData.split(":")[5]);
						
						java.sql.Statement st = null;
						st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
								ResultSet.CONCUR_READ_ONLY);
						
						st.executeQuery("UPDATE user SET Name='" + newName + "', School='" + newSchool
								+ "',Grade=" + newGrade + ",Class=" + newClass + ",Number=" + newNumber +
								"WHERE User='" + User + "'");
						
						st.close();
						  
					} catch (IOException e) {
						e.printStackTrace();
					} catch (SQLException ex) {
						Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
					}
			}


			if (readOPData == OP_EXIT) {// exit
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
			java.sql.Statement st = null;
			ResultSet rs = null;
			st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

			if (st.execute("SELECT * FROM club where Name='" + ID + "'")) {// "ID"�� ��ġ�ϴ� �����͸� �ҷ�����.
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
					if (st.execute("SELECT Balance FROM user where User='" + student + "'")) {
						rs = st.getResultSet();
					}
					// �ܾ� �߰� if�� �Է�

					while (rs.next()) {
						Balance = rs.getInt(1);
						System.out.println("Balance : " + Balance);
					}

					// ��ǰ������ȸ
					if (st.execute("SELECT Price FROM goods where Goods_Num='" + Goods_Num + "'")) {
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
						st.execute("INSERT INTO transactions (User, Club_Num, Goods_Num) VALUES ('" + student + "',"
								+ Club_Num + "," + Goods_Num + ")");

						// Balance����
						st.execute("UPDATE user set Balance=(Balance-" + Price + ") where User='" + student + "'");

						// ���Ƹ� ��������
						st.execute("UPDATE club set Income=(Income+" + Price + ") where Club_Num='" + Club_Num + "'");

						//// �⼮üũ
						// ���ŷ�Ž���ý���(FDS) 10���̳��� �ߺ� �⼮üũ ����
						String LastCheckTime = null;
						java.util.Date LastCheckT = null;

						if (st.execute("SELECT Time FROM atd_history where User='" + student + "'")) {
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
							if (st.execute("SELECT Time FROM atd_history where User='" + student + "'and Club_Num ="
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
								st.execute("UPDATE user set Visits=(Visits+1) Where User='" + student + "'");
								st.execute("UPDATE club set Visits=(Visits+1) where Club_Num='" + Club_Num + "'");// �ʿ伺
																													// �ǹ�?
							} else {// �������Ƹ����� 2ȸ�̻� �⼮üũ �õ�

							}
						} else {// 10���̳��� ������ �⼮üũ �õ�

						}
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

			if (st.execute("SELECT * FROM user where User='" + readData + "'")) {// ���ڵ忡 ��ġ�ϴ� �л������� �ҷ�����
				rs = st.getResultSet();
			}
			// ����̵�Ͻ� X
			if (!rs.next()) {// �ҷ��� �л������Ͱ� ���� ��
				// User����Ʈ�� �߰�
				rs.close();
				st.execute("INSERT INTO user (User) VALUES ('" + readData + "')");
				if (st.execute("SELECT * FROM user where User='" + readData + "'")) {
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

			if (st.execute("SELECT * FROM goods where Club_Num='" + Club_Num + "'")) {// ���Ƹ�������ȣ�� ��ġ�ϴ� ��ǰ�� �ҷ�����
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
		return toReturn.toArray(new String[toReturn.size()]);
	}

	String[] getRefundList(String readData) {
		java.sql.Statement st = null;
		ResultSet rs = null;
		ArrayList<String> toReturn = new ArrayList<>();
		try {

			st = CheongwonPayServer.con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

			if (st.execute(
					"SELECT * FROM transactions where Club_Num='" + Club_Num + "' and User='" + readData + "'")) {
				rs = st.getResultSet();
			}

			while (rs.next()) {
				toReturn.add(rs.getString("Time") + ":" + rs.getString("Goods_Num"));
			}
			toReturn.add(User.OP_GET_GOODS_LIST_FIN);// ��ǰ�����������OP

			st.close();
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
			if (st.execute("SELECT Cancel FROM transactions where Num='" + readData + "'")) {
				rs = st.getResultSet();
			}
			while (rs.next()) {
				temp[0] = rs.getString(1);// ���� SQL������ NCHAR���� �������� �ʾƼ� �ٲ�.
				System.out.println(temp[0]);
			}

			if (temp[0] == null) {
				// Num������ user���ڵ�Ȯ��
				if (st.execute("SELECT User FROM transactions where Num='" + readData + "'")) {
					rs = st.getResultSet();
				}
				while (rs.next()) {
					temp[1] = rs.getString(1);
					System.out.println(temp[1]);
				}

				// Num������ ����Ȯ��
				if (st.execute("SELECT Price FROM transactions where Num='" + readData + "'")) {
					rs = st.getResultSet();
				}
				while (rs.next()) {
					temp[2] = rs.getString(1);
					System.out.println(temp[2]);
				}

				// �ŷ����ǥ��
				if (st.execute("UPDATE transactions set Cancel = '1' where Num='" + readData + "'"))
					;

				// Balance ����
				if (st.execute("UPDATE user set Balance = (Balance-" + temp[2] + " where User='" + temp[1] + "'"))
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
			st.execute("UPDATE user set Club_Num='" + Club_Num + "' where User='" + readData + "'");// ���ڵ忡
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

				st.execute("UPDATE goods set Goods_Name='" + Goods_Name + "', Price='" + Price + "' where Goods_Num='"
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

				st.execute("DELETE From goods where Goods_Num='" + readData + "'");
				st.close();
			}
		} catch (SQLException ex) {
			Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
