import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;

public class Proxy {
	public static final int PORT = 4000;
	public static final char END = (char)3;
	
	public static void main(String[] args){
		Proxy proxy = new Proxy();
		try {
			proxy.connect();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private ServerSocket serverSocket;
	private ArrayList<Socket> clients;
	
	public Proxy(){
		clients = new ArrayList<Socket>();
	}
	
	public void connect() throws IOException {
		serverSocket = new ServerSocket(PORT);
		while(true){
			System.out.println("Waiting for connection...");
			Socket socket = serverSocket.accept();
			System.out.println("Connected to " + socket.getInetAddress().toString());
			spawnClientThread(socket);
		}
	}
	
	private void spawnClientThread(Socket socket){
		clients.add(socket);
		Runnable run = new Runnable(){
			@Override
			public void run() {
				try {
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					PrintWriter out = new PrintWriter(socket.getOutputStream());
					String s;
					while((s = in.readLine()) != null){
						String html = getHTML(s);
						out.println(html);
						out.println(END);
						out.flush();
						//System.out.println(s + " : " + END);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		new Thread(run).start();
	}
	
	public static String getHTML(String site){
		try{
			URL url = new URL(site);
			try(Scanner scanner = new Scanner(url.openStream())){
				StringBuilder content = new StringBuilder();
				while(scanner.hasNextLine()) content.append(scanner.nextLine());
				return content.toString();
			}
			catch(IOException i) {
				return errorPage;
			}
		}
		catch(Exception e){
			e.printStackTrace();
			return errorPage;
		}
	}
	
	public static final String errorPage = "<html><body><center><h1>An errror occurred.</h1></center></body></html>";
}
