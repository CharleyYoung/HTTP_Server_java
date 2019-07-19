package Http;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Yang Jian
 * HttpServer 服务器类
 */
public class HttpServer {
	private static String HOST = "127.0.0.1";
	ServerSocket serverSocket;//套接字
	private final int PORT = 80; // TCP服务器端口
	ExecutorService executorService; // 线程池
	final int POOL_SIZE = 10; // 单个处理器线程池工作线程数目
	public String rootpath;

	/**
	 * HttpServer 重载构造函数
	 * @param args 传入的参数，用于设置rootpath
	 * @throws IOException
	 */
	public HttpServer(String[] args) throws IOException {
		serverSocket = new ServerSocket(PORT,10); // 创建TCP服务器端套接字
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * POOL_SIZE);
		this.rootpath = args[0];
		System.out.println("服务器启动。");

	}

	public static void main(String[] args) throws IOException {
		new HttpServer(args).servic();
	}

	/**
	 * 服务的实现
	 */
	public void servic() {
		Socket socket = null;
		while (true) {
			try {
				socket = serverSocket.accept();
				System.out.println("新连接，连接地址：" + socket.getInetAddress() + "：" + socket.getPort());
				executorService.execute(new Handler(socket, rootpath));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
