package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class ChatServerThread extends Thread {
	private String nickname;
	private Socket socket;
	private List<Writer> listWriters;

	public ChatServerThread(Socket socket, List<Writer> listWriters) {
		this.socket = socket;
		this.listWriters = listWriters;
	}

	@Override
	public void run() {
		// 1. Remote Host Information
		InetSocketAddress inetRemoteSocketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
		String remoteHostAddress = inetRemoteSocketAddress.getAddress().getHostAddress();
		int remotePort = inetRemoteSocketAddress.getPort();
		ChatServer.log("connected by client[" + remoteHostAddress + ":" + remotePort + "]");

		// 2. 스트림 얻기
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
			PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"), true);

			while (true) {
				String request = bufferedReader.readLine();
				if (request == null) {
					ChatServer.log("클라이언트로부터 연결 끊김");
					doQuit(printwriter);
					break;
				}

				// 4. 프로토콜 분석
				String[] tokens = request.split(":");

				if ("join".equals(tokens[0])) {
					doJoin(tokens[1], printwriter);

				} else if ("message".equals(tokens[0])) {
					doMessage(tokens[1], printwriter);

				} else if ("quit".equals(tokens[0])) {
					doQuit(printwriter);

				} else {
					ChatServer.log("에러: 알수 없는 요청 (" + tokens[0] + ")");
				}
			}

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (socket != null && socket.isClosed() == false) {
					socket.close();
				}

			} catch (IOException ex) {
				ChatServer.log("error: " + ex);
			}
		}

	}

	private void doJoin(String nickName, Writer writer) {
		this.nickname = nickName;

		PrintWriter printWriter = (PrintWriter) writer;

		String data = "message:" + nickName + "님이 참여하였습니다.";
		broadcast(data);

		/* writer pool에 저장 */
		addWriter(writer);

		// ack
		printWriter.println("join:ok");
		printWriter.flush();

	}

	private void addWriter(Writer writer) {
		synchronized (listWriters) {
			listWriters.add(writer);
		}
	}

	private void broadcast(String data) {
		synchronized (listWriters) {
			for (Writer writer : listWriters) {
				PrintWriter printWriter = (PrintWriter) writer;
				printWriter.println(data);
				printWriter.flush();
			}
		}
	}

	private void doMessage(String message, Writer writer) {
		PrintWriter printWriter = (PrintWriter) writer;

		// base64로 변경하기
		printWriter.println("message:" + nickname + ":" + message);

	}

	private void doQuit(Writer writer) {
		PrintWriter printWriter = (PrintWriter) writer;

		removeWriter(printWriter);

		String data = "quit:" + nickname + "님이 퇴장 하였습니다.";
		broadcast(data);
	}

	private void removeWriter(Writer writer) {
		listWriters.remove(writer);
	}

}