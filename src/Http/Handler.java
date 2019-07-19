package Http;

import java.io.*;
import java.net.Socket;

/**
 * @author Yang Jian
 * Handler类，用于处理线程
 */
public class Handler implements Runnable { // 负责与单个客户通信的线程
	private Socket socket;// TCP套接字
	BufferedReader br;
	BufferedWriter bw;
	BufferedInputStream istream;
	BufferedOutputStream ostream;
	int port = 80;// 服务器端口
	public String rootpath;
	static private String CRLF = "\r\n";
	public String response_header = null;

	/**
	 * Handler 重载构造函数
	 * @param socket 传入的socket
	 * @param rootpath 从Server传入的rootpath
	 * @throws IOException
	 */
	public Handler(Socket socket, String rootpath) throws IOException {
		this.socket = socket;
		this.rootpath = rootpath;
	}

	/**
	 *initStream 用于初始化输入输出流
	 * @throws IOException
	 */
	public void initStream() throws IOException { // 初始化输入输出流对象方法
		br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		istream = new BufferedInputStream(socket.getInputStream());
		ostream = new BufferedOutputStream(socket.getOutputStream());
	}

	/**
	 * close 用于关闭输入输出流以及socket
	 * @throws IOException
	 */
	private void close() throws IOException{
		br.close();
		bw.close();
		istream.close();
		ostream.close();
		socket.close();
	}

	/**
	 * 执行函数
	 */
	public void run() { // 执行的内容
		try {
			initStream(); // 初始化输入输出流对象
			String info = null;
			info = br.readLine();
			if (info.startsWith("GET")) {
				handlerGET(info);
				this.close();
			} else if (info.startsWith("PUT")) {
				handlerPUT(info);
				this.close();
			}else{//既不是GET也不是PUT的请求，返回400
				doBadRequest();
				this.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * handlerGET 处理GET请求
	 * @param request 从客户端发来的请求行
	 * @throws IOException
	 */
	private void handlerGET(String request) throws IOException {
		System.out.println(request);// 打印请求行
		String[] part = request.split(" ");//对请求进行裁剪分段
		if(part.length!=3){//如果不是method+URL+http version的类型，返回400
			doBadRequest();
		}else if(part[2].equals("HTTP/1.0") || part[2].equals("HTTP/1.1")) {//是可接受的HTTP版本
			String filename = part[1];
			if (filename.equals("/")) {//如果什么都没写，返回index
				System.out.println("请求index");
				doIndex();

			} else {
				String filepath = rootpath + filename.replaceAll("/", "\\\\");//定义要返回的文件路径
				System.out.println(filepath);
				File file = new File(filepath);
				if (!file.exists()) {//文件不存在,返回404
					System.out.println("文件不存在");
					doNotFound();
					return;
				}
				FileInputStream fi = new FileInputStream(file);
				String content_type = null;
				if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {//判断请求类型是否支持，不支持返回400
					System.out.println("请求图片");
					content_type = "image/jpg";
				} else if (filename.endsWith(".html") || filename.endsWith(".htm")) {
					content_type = "text/html";
				} else {
					doBadRequest();
					content_type = "other";
				}
				int content_length = fi.available(), buffer_length = 0;

				//构造header
				byte[] buffer = new byte[8192];
				response_header = "HTTP/1.1 200 OK" + CRLF;
				response_header += "Content-Length:" + content_length + CRLF;
				response_header += "Content-Type:" + content_type + CRLF;
				response_header += CRLF;
				buffer = response_header.getBytes();
				ostream.write(buffer, 0, response_header.length());
				ostream.flush();

				//下面开始传输文件
				buffer = new byte[8192];
				while ((buffer_length = fi.read(buffer)) != -1) {
					ostream.write(buffer, 0, buffer_length);
					// System.out.println(buffer_length);
					ostream.flush();
				}
				System.out.println("成功传输文件");
				fi.close();
			}
		}else{
			doBadRequest();
		}
	}

	/**
	 * handlerPUT 处理PUT请求
	 * @param request 从客户端发来的请求行
	 * @throws IOException
	 */
	private void handlerPUT(String request) throws IOException {
		System.out.println(request);// 打印请求行
		String[] part = request.split(" ");//对请求进行裁剪分段
		if(part.length!=3){//如果不是method+URL+http version的类型，返回400
			doBadRequest();
		}else if(part[2].equals("HTTP/1.0") || part[2].equals("HTTP/1.1")) {//是可接受的HTTP版本
			String filename = part[1];;
			String filepath = rootpath + "\\saving" + filename.replaceAll("/","\\\\");//定义要返回的文件路径
			File file = new File(filepath);
			String response_header = "";
			response_header = "HTTP/1.1 201 Created" + CRLF;
			FileOutputStream fo = new FileOutputStream(filepath);
			System.out.println("Header:");
			char[] chars = new char[100];
			//使用char型数组解析字节流，使之转换成字符串
			String header_line = null;
			int line_length = 0, content_length = 0, i = 0, last = 0, c = 0;
			String content_type = "";
			boolean inHeader = true; // 判断是否在Header中
			while (inHeader && ((c = istream.read()) != -1)) {
				switch (c) {
					case '\r':
						line_length = i;// 字符个数
						header_line = new String(chars, 0, line_length);
						if (header_line.split(":")[0].equals("Content-Length")) {
							content_length = Integer.parseInt(header_line.split(":")[1]);
						}
						if (header_line.split(":")[0].equals("Content-Type")) {
							content_type = header_line.split(":")[1];
						}
						i = 0;
						line_length = 0;
						break;
					case '\n':
						if (c == last) {// ASCII码=0时表示字符为NULL
							inHeader = false;
							break;
						}
						last = c;// 最后一个字符为last
						System.out.print((char) c);
						break;
					default:
						chars[i] = (char) c;
						i++;
						last = c;
						System.out.print((char) c);// 追加c（强转为char）
				}
			}

			//接收文件
			int word_number = 0;
			while (word_number < content_length && (c = istream.read()) != -1) {
				fo.write(c);
				fo.flush();
				word_number++;
			}
			//使用content_length指定文件大小，终止循环
			System.out.println("文件写入完毕");
			fo.close();
			response_header += "Content-Length:" + content_length + CRLF;
			response_header += "Content-Type:" + content_type + CRLF;
			//System.out.println(header);
			ostream.write(response_header.getBytes(), 0, response_header.length());
			ostream.flush();
		}else {
			doBadRequest();
		}
	}

	/**
	 * doBadRequest 该函数负责返回BadRequest信息
	 */
	private void doBadRequest(){
		try {
			File file = new File(rootpath + "\\response\\400.html");//文件路径
			FileInputStream fi = new FileInputStream(file);
			int content_length = fi.available(), buffer_length = 0;
			//构造Header
			byte[] buffer = new byte[8192];
			response_header = "HTTP/1.1 400 Bad Request" + CRLF;
			response_header += "Content-Length:" + content_length + CRLF;
			response_header += "Content-Type:text/html" + CRLF;
			response_header += CRLF;
			buffer = response_header.getBytes();
			ostream.write(buffer, 0, response_header.length());
			ostream.flush();
			//开始传输
			buffer = new byte[8192];
			while ((buffer_length = fi.read(buffer)) != -1) {
				ostream.write(buffer, 0, buffer_length);
				ostream.flush();
			}
			System.out.println("成功传输文件");
			fi.close();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * doNotFound 该函数负责返回 Not Found信息
	 */
	private void doNotFound() {
		try {
			File file = new File(rootpath + "\\response\\404.html");
			FileInputStream fi = new FileInputStream(file);
			int content_length = fi.available(), buffer_length = 0;
			//构造Header
			byte[] buffer = new byte[8192];
			response_header = "HTTP/1.1 404 Not Found" + CRLF;
			response_header += "Content-Length:" + content_length + CRLF;
			response_header += "Content-Type:text/html" + CRLF;
			response_header += CRLF;
			buffer = response_header.getBytes();
			ostream.write(buffer, 0, response_header.length());
			ostream.flush();
			//开始传输
			buffer = new byte[8192];
			while ((buffer_length = fi.read(buffer)) != -1) {
				ostream.write(buffer, 0, buffer_length);
				ostream.flush();
			}
			System.out.println("成功传输文件");
			fi.close();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * doIndex 该函数负责返回index信息
	 */
	private void doIndex(){
		try {
			File file = new File(rootpath + "\\index.html");
			FileInputStream fi = new FileInputStream(file);
			int content_length = fi.available(), buffer_length = 0;
			//构造Header
			byte[] buffer = new byte[8192];
			response_header = "HTTP/1.1 200 OK" + CRLF;
			response_header += "Content-Length:" + content_length + CRLF;
			response_header += "Content-Type:text/html" + CRLF;
			response_header += CRLF;
			buffer = response_header.getBytes();
			ostream.write(buffer, 0, response_header.length());
			ostream.flush();
			//开始传输
			buffer = new byte[8192];
			while ((buffer_length = fi.read(buffer)) != -1) {
				ostream.write(buffer, 0, buffer_length);
				ostream.flush();
			}
			System.out.println("成功传输文件");
			fi.close();
		}catch (Exception e){
			e.printStackTrace();
		}
	}
}
