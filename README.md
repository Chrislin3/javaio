# BIO NIO AIO演变
## BIO
#### BIO 概述
BIO 全称Block-IO 是一种阻塞同步的通信模式。我们常说的Stock IO
一般指的是BIO。是一个比较传统的通信方式，模式简单，使用方便。但并发处理能力低，通信耗时，依赖网速。
#### BIO 设计原理：
服务器通过一个Acceptor线程负责监听客户端请求和为每个客户端创建一个新的线程进行链路处理。典型的一请求一应答模式。若客户端数量增多，频繁地创建和销毁线程会给服务器打开很大的压力。后改良为用线程池的方式代替新增线程，被称为伪异步IO。

服务器提供IP地址和监听的端口，客户端通过TCP的三次握手与服务器连接，连接成功后，双放才能通过套接字(Stock)通信。
#### 小结
BIO模型中通过Socket和ServerSocket完成套接字通道的实现。阻塞，同步，建立连接耗时。
#### BIO服务器代码
负责启动服务，阻塞服务，监听客户端请求，新建线程处理任务。

```
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/**
 * IO 也称为 BIO，Block IO 阻塞同步的通讯方式
 * 比较传统的技术，实际开发中基本上用Netty或者是AIO。熟悉BIO，NIO，体会其中变化的过程。作为一个web开发人员，stock通讯面试经常问题。
 * BIO最大的问题是：阻塞，同步。
 * BIO通讯方式很依赖于网络，若网速不好，阻塞时间会很长。每次请求都由程序执行并返回，这是同步的缺陷。
 * BIO工作流程：
 * 第一步：server端服务器启动
 * 第二步：server端服务器阻塞监听client请求
 * 第三步：server端服务器接收请求，创建线程实现任务
 */
public class ITDragonBIOServer {
    
    private static final Integer PORT = 8888; // 服务器对外的端口号  
    public static void main(String[] args) {  
        ServerSocket server = null;  
        Socket socket = null;  
        ThreadPoolExecutor executor = null;  
        try {  
            server = new ServerSocket(PORT); // ServerSocket 启动监听端口  
            System.out.println("BIO Server 服务器启动.........");  
            /*--------------传统的新增线程处理----------------*/
            /*while (true) { 
                // 服务器监听：阻塞，等待Client请求 
                socket = server.accept(); 
                System.out.println("server 服务器确认请求 : " + socket); 
                // 服务器连接确认：确认Client请求后，创建线程执行任务  。很明显的问题，若每接收一次请求就要创建一个线程，显然是不合理的。
                new Thread(new ITDragonBIOServerHandler(socket)).start(); 
            } */
            /*--------------通过线程池处理缓解高并发给程序带来的压力（伪异步IO编程）----------------*/  
            executor = new ThreadPoolExecutor(10, 100, 1000, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(50));  
            while (true) {  
                socket = server.accept();  // 服务器监听：阻塞，等待Client请求 
                ITDragonBIOServerHandler serverHandler = new ITDragonBIOServerHandler(socket);  
                executor.execute(serverHandler);  
            }  
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally {  
            try {  
                if (null != socket) {  
                  socket.close(); 
                  socket = null;
                }  
                if (null != server) {  
                    server.close();  
                    server = null;  
                    System.out.println("BIO Server 服务器关闭了！！！！");  
                }  
                executor.shutdown();  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
        }  
    }  
}
```


#### BIO服务端处理任务代码
负责处理Stock套接字，返回套接字给客户端，解耦。

```
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ITDragonBIOServerHandler implements Runnable{

    private Socket socket;
    public ITDragonBIOServerHandler(Socket socket) {
        this.socket = socket;
    }
    @Override
    public void run() {
        BufferedReader reader = null;
        PrintWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            writer = new PrintWriter(this.socket.getOutputStream(), true);
            String body = null;
            while (true) {
                body = reader.readLine(); // 若客户端用的是 writer.print() 传值，那readerLine() 是不能获取值，细节
                if (null == body) {
                    break;
                }
                System.out.println("server服务端接收参数 : " + body);
                writer.println(body + " = ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != writer) {
                writer.close();
            }
            try {
                if (null != reader) {
                    reader.close();
                }
                if (null != this.socket) {
                    this.socket.close();
                    this.socket = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```


#### BIO客户端代码
负责启动客户端，向服务器发送请求，接收服务器返回的Stock套接字。

```
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
/**
 * BIO 客户端
 * Socket :         向服务端发送连接
 * PrintWriter :     向服务端传递参数
 * BufferedReader : 从服务端接收参数
 */
public class ITDragonBIOClient {
    
    private static Integer PORT = 8888;  
    private static String IP_ADDRESS = "127.0.0.1";  
    public static void main(String[] args) {  
        for (int i = 0; i < 10; i++) {  
            clientReq(i);  
        }  
    }  
    private static void clientReq(int i) {  
        Socket socket = null;  
        BufferedReader reader = null;  
        PrintWriter writer = null;  
        try {  
            socket = new Socket(IP_ADDRESS, PORT); // Socket 发起连接操作。连接成功后，双方通过输入和输出流进行同步阻塞式通信  
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); // 获取返回内容  
            writer = new PrintWriter(socket.getOutputStream(), true);  
            String []operators = {"+","-","*","/"};
            Random random = new Random(System.currentTimeMillis());  
            String expression = random.nextInt(10)+operators[random.nextInt(4)]+(random.nextInt(10)+1);
            writer.println(expression); // 向服务器端发送数据  
            System.out.println(i + " 客户端打印返回数据 : " + reader.readLine());  
        } catch (Exception e) {  
            e.printStackTrace();  
        } finally {  
            try {  
                if (null != reader) {  
                    reader.close();  
                }  
                if (null != socket) {  
                    socket.close();  
                    socket = null;  
                }  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
        }  
    }  
}
```


## NIO
#### NIO 概述
NIO 全称New IO，也叫Non-Block IO 是一种非阻塞同步的通信模式。
NIO 设计原理：
NIO 相对于BIO来说一大进步。客户端和服务器之间通过Channel通信。NIO可以在Channel进行读写操作。这些Channel都会被注册在Selector多路复用器上。Selector通过一个线程不停的轮询这些Channel。找出已经准备就绪的Channel执行IO操作。
NIO 通过一个线程轮询，实现千万个客户端的请求，这就是非阻塞NIO的特点。
#### 1）缓冲区Buffer：
它是NIO与BIO的一个重要区别。BIO是将数据直接写入或读取到Stream对象中。而NIO的数据操作都是在缓冲区中进行的。缓冲区实际上是一个数组。Buffer最常见的类型是ByteBuffer，另外还有CharBuffer，ShortBuffer，IntBuffer，LongBuffer，FloatBuffer，DoubleBuffer。
#### 2）通道Channel：
和流不同，通道是双向的。NIO可以通过Channel进行数据的读，写和同时读写操作。通道分为两大类：一类是网络读写（SelectableChannel），一类是用于文件操作（FileChannel），我们使用的SocketChannel和ServerSocketChannel都是SelectableChannel的子类。
#### 3）多路复用器Selector：
NIO编程的基础。多路复用器提供选择已经就绪的任务的能力。就是Selector会不断地轮询注册在其上的通道（Channel），如果某个通道处于就绪状态，会被Selector轮询出来，然后通过SelectionKey可以取得就绪的Channel集合，从而进行后续的IO操作。服务器端只要提供一个线程负责Selector的轮询，就可以接入成千上万个客户端，这就是JDK NIO库的巨大进步。
##### 说明：这里的代码只实现了客户端发送请求，服务端接收数据的功能。其目的是简化代码，方便理解。github源码中有完整代码。
#### 小结：
NIO模型中通过SocketChannel和ServerSocketChannel完成套接字通道的实现。非阻塞/阻塞，同步，避免TCP建立连接使用三次握手带来的开销。
#### NIO服务器代码：
负责开启多路复用器，打开通道，注册通道，轮询通道，处理通道。

```
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
/**
 * NIO 也称 New IO， Non-Block IO，非阻塞同步通信方式
 * 从BIO的阻塞到NIO的非阻塞，这是一大进步。功归于Buffer，Channel，Selector三个设计实现。
 * Buffer   ：  缓冲区。NIO的数据操作都是在缓冲区中进行。缓冲区实际上是一个数组。而BIO是将数据直接写入或读取到Stream对象。
 * Channel  ：  通道。NIO可以通过Channel进行数据的读，写和同时读写操作。
 * Selector ：  多路复用器。NIO编程的基础。多路复用器提供选择已经就绪状态任务的能力。
 * 客户端和服务器通过Channel连接，而这些Channel都要注册在Selector。Selector通过一个线程不停的轮询这些Channel。找出已经准备就绪的Channel执行IO操作。
 * NIO通过一个线程轮询，实现千万个客户端的请求，这就是非阻塞NIO的特点。
 */
public class ITDragonNIOServer implements Runnable{  
    
  private final int BUFFER_SIZE = 1024; // 缓冲区大小  
  private final int PORT = 8888;         // 监听的端口  
  private Selector selector;              // 多路复用器，NIO编程的基础，负责管理通道Channel 
  private ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);  // 缓冲区Buffer  
  public ITDragonNIOServer() {  
      startServer();  
  }  
  private void startServer() {  
      try {  
          // 1.开启多路复用器  
          selector = Selector.open();  
          // 2.打开服务器通道(网络读写通道)  
          ServerSocketChannel channel = ServerSocketChannel.open();  
          // 3.设置服务器通道为非阻塞模式，true为阻塞，false为非阻塞  
          channel.configureBlocking(false);  
          // 4.绑定端口  
          channel.socket().bind(new InetSocketAddress(PORT));  
          // 5.把通道注册到多路复用器上，并监听阻塞事件  
          /** 
           * SelectionKey.OP_READ     : 表示关注读数据就绪事件  
           * SelectionKey.OP_WRITE     : 表示关注写数据就绪事件  
           * SelectionKey.OP_CONNECT: 表示关注socket channel的连接完成事件  
           * SelectionKey.OP_ACCEPT : 表示关注server-socket channel的accept事件  
           */  
          channel.register(selector, SelectionKey.OP_ACCEPT);  
          System.out.println("Server start >>>>>>>>> port :" + PORT);  
      } catch (IOException e) {  
          e.printStackTrace();  
      }  
  }  
  // 需要一个线程负责Selector的轮询  
  @Override  
  public void run() {  
      while (true) {  
          try {  
              /** 
               * a.select() 阻塞到至少有一个通道在你注册的事件上就绪  
               * b.select(long timeOut) 阻塞到至少有一个通道在你注册的事件上就绪或者超时timeOut 
               * c.selectNow() 立即返回。如果没有就绪的通道则返回0  
               * select方法的返回值表示就绪通道的个数。 
               */  
              // 1.多路复用器监听阻塞  
              selector.select();  
              // 2.多路复用器已经选择的结果集  
              Iterator<SelectionKey> selectionKeys = selector.selectedKeys().iterator();  
              // 3.不停的轮询  
              while (selectionKeys.hasNext()) {  
                  // 4.获取一个选中的key  
                  SelectionKey key = selectionKeys.next();  
                  // 5.获取后便将其从容器中移除  
                  selectionKeys.remove();  
                  // 6.只获取有效的key  
                  if (!key.isValid()){  
                      continue;  
                  }  
                  // 阻塞状态处理  
                  if (key.isAcceptable()){  
                      accept(key);  
                  }  
                  // 可读状态处理  
                  if (key.isReadable()){  
                      read(key);  
                  }  
              }  
          } catch (IOException e) {  
              e.printStackTrace();  
          }  
      }  
  }  
  // 设置阻塞，等待Client请求。在传统IO编程中，用的是ServerSocket和Socket。
  //在NIO中采用的ServerSocketChannel和SocketChannel  
  private void accept(SelectionKey selectionKey) {  
      try {  
          // 1.获取通道服务  
          ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();  
          // 2.执行阻塞方法  
          SocketChannel socketChannel = serverSocketChannel.accept();  
          // 3.设置服务器通道为非阻塞模式，true为阻塞，false为非阻塞  
          socketChannel.configureBlocking(false);  
          // 4.把通道注册到多路复用器上，并设置读取标识  
          socketChannel.register(selector, SelectionKey.OP_READ);  
      } catch (IOException e) {  
          e.printStackTrace();  
      }  
  }  
  private void read(SelectionKey selectionKey) {  
      try {  
          // 1.清空缓冲区数据  
          readBuffer.clear();  
          // 2.获取在多路复用器上注册的通道  
          SocketChannel socketChannel = (SocketChannel) selectionKey.channel();  
          // 3.读取数据，返回  
          int count = socketChannel.read(readBuffer);  
          // 4.返回内容为-1 表示没有数据  
          if (-1 == count) {  
              selectionKey.channel().close();  
              selectionKey.cancel();  
              return ;  
          }  
          // 5.有数据则在读取数据前进行复位操作  
          readBuffer.flip();  
          // 6.根据缓冲区大小创建一个相应大小的bytes数组，用来获取值  
          byte[] bytes = new byte[readBuffer.remaining()];  
          // 7.接收缓冲区数据  
          readBuffer.get(bytes);  
          // 8.打印获取到的数据  
          System.out.println("NIO Server : " + new String(bytes)); // 不能用bytes.toString()  
      } catch (IOException e) {  
          e.printStackTrace();  
      }  
  }  
  public static void main(String[] args) {  
      new Thread(new ITDragonNIOServer()).start();  
  } 
}
```

#### NIO客户端代：
负责连接服务器，声明通道，连接通道：

```
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ITDragonNIOClient {
    
    private final static int PORT = 8888;  
    private final static int BUFFER_SIZE = 1024;  
    private final static String IP_ADDRESS = "127.0.0.1";  
    public static void main(String[] args) {  
        clientReq();
    }  
    private static void clientReq() {
        // 1.创建连接地址  
        InetSocketAddress inetSocketAddress = new InetSocketAddress(IP_ADDRESS, PORT);  
        // 2.声明一个连接通道  
        SocketChannel socketChannel = null;  
        // 3.创建一个缓冲区  
        ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);  
        try {  
            // 4.打开通道  
            socketChannel = SocketChannel.open();  
            // 5.连接服务器  
            socketChannel.connect(inetSocketAddress);  
            while(true){  
                // 6.定义一个字节数组，然后使用系统录入功能：  
                byte[] bytes = new byte[BUFFER_SIZE];  
                // 7.键盘输入数据  
                System.in.read(bytes);  
                // 8.把数据放到缓冲区中  
                byteBuffer.put(bytes);  
                // 9.对缓冲区进行复位  
                byteBuffer.flip();  
                // 10.写出数据  
                socketChannel.write(byteBuffer);  
                // 11.清空缓冲区数据  
                byteBuffer.clear();  
            }  
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally {  
            if (null != socketChannel) {  
                try {  
                    socketChannel.close();  
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            }  
        } 
    }
}
```

## AIO
#### AIO概述
AIO 也叫NIO2.0 是一种非阻塞异步的通信模式。在NIO的基础上引入了新的异步通道的概念，并提供了异步文件通道和异步套接字通道的实现。

AIO 并没有采用NIO的多路复用器，而是使用异步通道的概念。其read，write方法的返回类型都是Future对象。而Future模型是异步的，其核心思想是：去主函数等待时间。
#### 小结：
AIO模型中通过AsynchronousSocketChannel和AsynchronousServerSocketChannel完成套接字通道的实现。非阻塞，异步。
#### AIO服务端代码
负责创建服务器通道，绑定端口，等待请求。

```
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 * AIO, 也叫 NIO2.0 是一种异步非阻塞的通信方式
 * AIO 引入了异步通道的概念 AsynchronousServerSocketChannel和AsynchronousSocketChannel 其read和write方法返回值类型是Future对象。
 */
public class ITDragonAIOServer {
      
    private ExecutorService executorService;          // 线程池
    private AsynchronousChannelGroup threadGroup;      // 通道组
    public AsynchronousServerSocketChannel asynServerSocketChannel;  // 服务器通道 
    public void start(Integer port){  
        try {  
            // 1.创建一个缓存池  
            executorService = Executors.newCachedThreadPool();  
            // 2.创建通道组  
            threadGroup = AsynchronousChannelGroup.withCachedThreadPool(executorService, 1);  
            // 3.创建服务器通道  
            asynServerSocketChannel = AsynchronousServerSocketChannel.open(threadGroup);  
            // 4.进行绑定  
            asynServerSocketChannel.bind(new InetSocketAddress(port));  
            System.out.println("server start , port : " + port);  
            // 5.等待客户端请求  
            asynServerSocketChannel.accept(this, new ITDragonAIOServerHandler());  
            // 一直阻塞 不让服务器停止，真实环境是在tomcat下运行，所以不需要这行代码  
            Thread.sleep(Integer.MAX_VALUE);  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    }  
    public static void main(String[] args) {  
        ITDragonAIOServer server = new ITDragonAIOServer();  
        server.start(8888);  
    }  
}
```


#### AIO服务器任务处理代码
负责，读取数据，写入数据

```
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import com.itdragon.util.CalculatorUtil;

public class ITDragonAIOServerHandler implements CompletionHandler<AsynchronousSocketChannel, ITDragonAIOServer> {  
  private final Integer BUFFER_SIZE = 1024;  
  @Override  
  public void completed(AsynchronousSocketChannel asynSocketChannel, ITDragonAIOServer attachment) {  
      // 保证多个客户端都可以阻塞  
      attachment.asynServerSocketChannel.accept(attachment, this);  
      read(asynSocketChannel);  
  }  
  //读取数据  
  private void read(final AsynchronousSocketChannel asynSocketChannel) {  
      ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);  
      asynSocketChannel.read(byteBuffer, byteBuffer, new CompletionHandler<Integer, ByteBuffer>() {  
          @Override  
          public void completed(Integer resultSize, ByteBuffer attachment) {  
              //进行读取之后,重置标识位  
              attachment.flip();  
              //获取读取的数据  
              String resultData = new String(attachment.array()).trim();  
              System.out.println("Server -> " + "收到客户端的数据信息为:" + resultData);  
              String response = resultData + " = " + CalculatorUtil.cal(resultData);  
              write(asynSocketChannel, response);  
          }  
          @Override  
          public void failed(Throwable exc, ByteBuffer attachment) {  
              exc.printStackTrace();  
          }  
      });  
  }  
  // 写入数据
  private void write(AsynchronousSocketChannel asynSocketChannel, String response) {  
      try {  
          // 把数据写入到缓冲区中  
          ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);  
          buf.put(response.getBytes());  
          buf.flip();  
          // 在从缓冲区写入到通道中  
          asynSocketChannel.write(buf).get();  
      } catch (InterruptedException e) {  
          e.printStackTrace();  
      } catch (ExecutionException e) {  
          e.printStackTrace();  
      }  
  }  
  @Override  
  public void failed(Throwable exc, ITDragonAIOServer attachment) {  
      exc.printStackTrace();  
  }  
}
```


#### AIO客户端代码
负责连接服务器，声明通道，连接通道

```
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Random;

public class ITDragonAIOClient implements Runnable{  
    private static Integer PORT = 8888;  
    private static String IP_ADDRESS = "127.0.0.1";
    private AsynchronousSocketChannel asynSocketChannel ;  
    public ITDragonAIOClient() throws Exception {  
        asynSocketChannel = AsynchronousSocketChannel.open();  // 打开通道  
    }  
    public void connect(){  
        asynSocketChannel.connect(new InetSocketAddress(IP_ADDRESS, PORT));  // 创建连接 和NIO一样  
    }  
    public void write(String request){  
        try {  
            asynSocketChannel.write(ByteBuffer.wrap(request.getBytes())).get();  
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);  
            asynSocketChannel.read(byteBuffer).get();  
            byteBuffer.flip();  
            byte[] respByte = new byte[byteBuffer.remaining()];  
            byteBuffer.get(respByte); // 将缓冲区的数据放入到 byte数组中  
            System.out.println(new String(respByte,"utf-8").trim());  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    }  
    @Override  
    public void run() {  
        while(true){  
        }  
    }  
    public static void main(String[] args) throws Exception {  
        for (int i = 0; i < 10; i++) {
            ITDragonAIOClient myClient = new ITDragonAIOClient();  
            myClient.connect();  
            new Thread(myClient, "myClient").start(); 
            String []operators = {"+","-","*","/"};
            Random random = new Random(System.currentTimeMillis());  
            String expression = random.nextInt(10)+operators[random.nextInt(4)]+(random.nextInt(10)+1);
            myClient.write(expression);  
        }
    }  
}
```


## 常见面试题
#### 1、BIO，NIO，AIO区别
（1）BIO 就是传统的 java.io 包，它是基于流模型实现的，交互的方式是同步、阻塞方式，客户端和服务器连接需要三次握手，也就是说在读入输入流或者输出流时，在读写动作完成之前，线程会一直阻塞在那里，它们之间的调用时可靠的线性顺序。它的有点就是代码比较简单、直观；缺点就是 IO 的效率和扩展性很低，容易成为应用性能瓶颈。

（2）NIO 是 Java 1.4 引入的 java.nio 包，提供了 Channel、Selector、Buffer 等新的抽象，可以构建多路复用的、同步非阻塞 IO 程序，同时提供了更接近操作系统底层高性能的数据操作方式。NIO 非阻塞同步通信模式，客户端与服务器通过Channel连接，采用多路复用器轮询注册的Channel。提高吞吐量和可靠性。

（3）AIO 是 Java 1.7 之后引入的包，是 NIO 的升级版本，提供了异步非堵塞的 IO 操作方式，所以人们叫它 AIO（Asynchronous IO），异步 IO 是基于事件和回调机制实现的，也就是应用操作之后会直接返回，不会堵塞在那里，当后台处理完成，操作系统会通知相应的线程进行后续的操作。

#### 2、Stock通信的伪代码实现流程
服务器绑定端口：

```
server = new ServerSocket(PORT)
```

服务器阻塞监听：

```
socket = server.accept()
```

服务器开启线程：

```
new Thread(Handle handle)
```

服务器读写数据：

```
BufferedReader PrintWriter
```

客户端绑定IP和PORT：

```
new Socket(IP_ADDRESS, PORT)
```

客户端传输接收数据：

```
BufferedReader PrintWriter
```

#### 3、TCP协议与UDP协议有什么区别
TCP : 传输控制协议是基于连接的协议，在正式收发数据前，必须和对方建立可靠的连接。速度慢，合适传输大量数据。

UDP : 用户数据报协议是与TCP相对应的协议。面向非连接的协议，不与对方建立连接，而是直接就把数据包发送过去，速度快，适合传输少量数据。
#### 4 什么是同步阻塞BIO，同步非阻塞NIO，异步非阻塞AIO
同步阻塞IO : 用户进程发起一个IO操作以后，必须等待IO操作的真正完成后，才能继续运行。

同步非阻塞IO: 用户进程发起一个IO操作以后，可做其它事情，但用户进程需要经常询问IO操作是否完成，这样造成不必要的CPU资源浪费。

异步非阻塞IO: 用户进程发起一个IO操作然后，立即返回，等IO操作真正的完成以后，应用程序会得到IO操作完成的通知。类比Future模式。
## 总结
1、BIO模型中通过Socket和ServerSocket完成套接字通道实现。阻塞，同步，连接耗时。

2、NIO模型中通过SocketChannel和ServerSocketChannel完成套接字通道实现。非阻塞/阻塞，同步，避免TCP建立连接使用三次握手带来的开销。

3、AIO模型中通过AsynchronousSocketChannel和AsynchronousServerSocketChannel完成套接字通道实现。非阻塞，异步。

