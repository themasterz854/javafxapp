package sample;


import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.*;
import java.net.Socket;


public class ChatDialogController {
    @FXML
    private AnchorPane ap;
    @FXML
    private Button send_file_button,dirchoose;
    @FXML
    private TextField message;
    @FXML
    private TextArea myta,ta;
    @FXML
    private ToggleButton togglebutton;
    @FXML
    private Label encryplabel;
    private int id;
    private DataOutputStream dout ;
    private DataInputStream din;
    private final String[] queue= new String[10];
    private String[] filenames = new String[20];
    private boolean encryptflag = false;
    private File directory;
    private final DirectoryChooser dc = new DirectoryChooser();
    private Socket s;
    public void transferdata(int cid, Socket s){
        id = cid;
        this.s = s;
    }

    public void direchooser()
    {
        directory = dc.showDialog(null);
    }
    public synchronized void checkandwrite() throws Exception{
        int front,rear;
        front = rear = -1;
        String fileName;
        byte[] receivedData;
        FileOutputStream fos;
        while(din.available()> 0) {
            try {
                String str = din.readUTF();
                if (str.equals("%file%")) {
                    fileName = din.readUTF();
                    int fileSize = Integer.parseInt(din.readUTF());
                    receivedData = new byte[fileSize];
                    din.readFully(receivedData);
                    fos = new FileOutputStream(directory.getAbsolutePath() + "\\" + fileName);
                    fos.write(receivedData, 0, fileSize);
                    fos.close();
                } else {
                    String[] data = str.split(" ");
                    if (Integer.parseInt(data[0]) == id) {
                        for (int i = 1; i < data.length; i++)
                            ta.appendText(data[i] + " ");
                        ta.appendText("\n");
                        myta.appendText("\n");
                    } else {
                        if (front == -1) {
                            front = 0;
                        }
                        queue[++rear] = str;

                    }

                    while (front != rear + 1 && front != -1) {
                        dout.writeUTF("%others%" + " " + queue[front++]);
                    }
                }
            }catch (Exception e)
            {
                e.printStackTrace();

            }
        }
    }
     void run_task(){
        Task<Thread> task = new Task<>(){

            @Override
            protected Thread call() throws Exception {

                Stage stage ;
                din = new DataInputStream(s.getInputStream());
                dout = new DataOutputStream(s.getOutputStream());
                stage = (Stage) message.getScene().getWindow();

                while(true)
                {
                    while(!stage.isFocused())
                    {
                        Thread.sleep(200);
                    }
                    checkandwrite();
                    if(!stage.isShowing())
                    {
                        break;
                    }
                    Thread.sleep(100);

                }
                return null;
            }
        };
        new Thread(task).start();
    }

    public void changereceiver() throws Exception {
        dout = new DataOutputStream(s.getOutputStream());
        dout.writeUTF("%chat% "+id);
        dout.flush();
    }
    public void encryption_toggle() throws Exception {
        String str;
        if(togglebutton.isSelected()) {
            ap.getStyleClass().remove("chatbg");
            ap.getStyleClass().add("encryptchatbg");
            str = "%enableencryption%";
            dirchoose.setOpacity(0.0);
            dirchoose.setDisable(true);
            send_file_button.setOpacity(0.0);
            send_file_button.setDisable(true);
            encryplabel.setText("Encryption is ON");
            encryptflag = true;
        }
        else
        {
            ap.getStyleClass().remove("encryptchatbg");
            ap.getStyleClass().add("chatbg");
            encryptflag = false;
            dirchoose.setDisable(false);
            dirchoose.setOpacity(1.0);
            send_file_button.setOpacity(1.0);
            send_file_button.setDisable(false);
            encryplabel.setText("Encryption is OFF");
            str = "%disableencryption%";
        }
        dout.writeUTF(str);
        dout.flush();
        myta.clear();
        ta.clear();
    }
    public void send_message() throws Exception {
        dout = new DataOutputStream(s.getOutputStream());
        dout.writeUTF(message.getText());
        dout.flush();
        if(!encryptflag) {
            synchronized (myta) {
                ta.appendText("\n");
                myta.appendText(message.getText() + "\n");
            }
        }
        message.clear();
    }
    public void start_file_window(){
        try {
            Stage file_chooser = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FileChooser.fxml"));
            Parent root = loader.load();
            FileChooserController fcc = loader.getController();
            fcc.transferdata(s);
            fcc.senddata(filenames);
            Scene list_scene = new Scene(root);
            file_chooser.setScene(list_scene);
            file_chooser.setResizable(false);
            file_chooser.show();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
