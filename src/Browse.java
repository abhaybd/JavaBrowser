import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.concurrent.Worker.State;
import javafx.scene.input.*;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.commons.io.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;

public class Browse extends Application {

    static String version = "4.0.0"; //version of browser
    static String toGo = "http://www.google.com"; //url to go to. starts at google.com
    final static String google = "https://www.google.com/?safe=active&ssui=on#q="; //url stub for google search
    static boolean debug = true; //print debug statements?
    static boolean viewSource = false; //show html source?
    static boolean hyperDodge = true; //proxy enabled?
    static Socket socket = null; //socket to communicate to the server. This is the not-proxy method. (fallback)
    static final String SERVER = "104.236.111.140"; //ip of the server
    static final int PORT = 3128; //port to connect to.
    @Override
    public void start(Stage stage) throws Exception {
        final int maxTabs = 10; //starting tabs
        stage.setTitle("Dodge Browser - " + version); //set title, width and maximized

        stage.setWidth(1024);
        stage.setHeight(768);
        stage.setMaximized(true);

        //WebView browser[] = new WebView[maxTabs];
        ArrayList<WebView> browser = new ArrayList<>(); //initialize an arraylist of webviews
        Scene scene = new Scene(new Group()); //initialize scene layouts
        VBox root = new VBox();
        HBox url_layout = new HBox();
        TabPane tabs = new TabPane();
        //Tab[] tab = new Tab[maxTabs];
        ArrayList<Tab> tab = new ArrayList<Tab>(); //initialize arraylist of tabs
        tabs.setTabMinWidth(50);
        tabs.setPrefHeight(stage.getHeight());
        //this inits all the tabs to google.com. also inits the tab and browser array.
        for(int i = 0; i < maxTabs; i++){ //load up and initialize the tabs and webviews
            Tab temp = new Tab();
            WebView browser_temp = new WebView();
            browser_temp.getEngine().setUserAgent("Dodge");
            temp.setText("Tab");
            temp.setContent(browser_temp);
            temp.setId("tab");
            tabs.getTabs().add(temp);
            browser_temp.getEngine().load(toGo);
            browser.add(browser_temp);
            temp.setClosable(true);
            tab.add(temp);
        }

        Tab temp = new Tab(); //create 'new tab' tab.
        temp.setId("+");
        temp.setText("+");
        tabs.getTabs().add(temp);
        temp.setClosable(false);
        tab.add(temp);

        //set up the context menu and it's listeners (right click menu)
        ContextMenu menu = new ContextMenu();
        MenuItem reload = new MenuItem("Reload");
        reload.setOnAction(e -> browser.get(currentSelection(tabs)).getEngine().reload());
        MenuItem back = new MenuItem("Back");
        back.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                WebView temp = browser.get(currentSelection(tabs)); //get the current webview
                List<WebHistory.Entry> history= temp.getEngine().getHistory().getEntries(); //get history
                try {
                    String last = history.get(history.size() - 2).getUrl(); //get webpage before current
                    tab.get(currentSelection(tabs)).setId(temp.getEngine().getLocation()); //set id of tab to current url (for the forward function)
                    debug(last);
                    //temp.getEngine().load(last);
                    load(temp.getEngine(), last); //load page
                }catch(ArrayIndexOutOfBoundsException e){
                    debug("Nothing to go back to!");
                }

            }
        });
        MenuItem forward = new MenuItem("Forward"); //add forward to context menu
        forward.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                WebView temp = browser.get(currentSelection(tabs)); //get current webview

                String forward = tab.get(currentSelection(tabs)).getId(); //get the url to go forward to.
                if (!forward.equalsIgnoreCase("tab")) { //don't go forward if it's the default.
                    debug(forward);
                   //temp.getEngine().load(forward);
                    load(temp.getEngine(), forward); //load the page
                } else {
                    debug("Nothing to go forward to!");
                }

            }
        });
        MenuItem js = new MenuItem("Toggle JS: " + browser.get(0).getEngine().isJavaScriptEnabled()); //this will be a universal js button
        js.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                for(WebView temp:browser){
                    temp.getEngine().setJavaScriptEnabled(!temp.getEngine().isJavaScriptEnabled()); //toggle javascript
                }
                js.setText("Toggle JS: " + browser.get(0).getEngine().isJavaScriptEnabled()); //update button label
            }
        });
        menu.getItems().addAll(reload,back,forward,js); //add context options to menu
        for(WebView view:browser){
            view.setContextMenuEnabled(false); //disable the default context menu
        }

        //show the context menu on right cick, hide on left click.
        tabs.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                WebView temp = browser.get(currentSelection(tabs));
                if (event.getButton() == MouseButton.SECONDARY) {
                    menu.show(temp, event.getScreenX(), event.getScreenY());
                    debug("right click!");
                }
                if(event.getButton() == MouseButton.PRIMARY){
                    menu.hide();
                }
            }
        });

        //Create UI buttons/textboxes
        Button goBack = new Button("Back");
        Button goForward = new Button("Forward");
        Button reloadButton = new Button("Reload");
        Button source = new Button("View Source: " + viewSource);
        Button hyper = new Button("HyperDodge active: " + hyperDodge);
        Button go = new Button("Go");
        TextField url = new TextField();
        url.setPrefWidth(800);
        
        hyper.setOnAction(new EventHandler<ActionEvent>(){ //toggle proxy
			@Override
			public void handle(ActionEvent arg0) {
				hyperDodge = !hyperDodge;
				if(hyperDodge){
					showHyperDodgeDialog(); //show info dialog
					//set the system http and https proxy settings
					System.setProperty("http.proxyHost", SERVER);
		        	System.setProperty("http.proxyPort", String.valueOf(PORT));
		        	System.setProperty("https.proxyHost", SERVER);
		        	System.setProperty("https.proxyPort", String.valueOf(PORT));
		        	socket = null;
				}
				else{
					System.setProperty("http.proxyHost", "");
		        	System.setProperty("http.proxyPort", "");
		        	System.setProperty("https.proxyHost", "");
		        	System.setProperty("https.proxyPort", "");
				}
				hyper.setText("HyperDodge active: " + hyperDodge);
			}
        });
        
        reload.setOnAction(e -> browser.get(currentSelection(tabs)).getEngine().reload()); //reload webpage

        goBack.setOnAction(new EventHandler<ActionEvent>() { //same thing as context menu, just for a button. I definitely should have made this into a method.
            @Override
            public void handle(ActionEvent event) {
                WebView temp = browser.get(currentSelection(tabs));
                List<WebHistory.Entry> history= temp.getEngine().getHistory().getEntries();
                try {
                    String last = history.get(history.size() - 2).getUrl();
                    tab.get(currentSelection(tabs)).setId(temp.getEngine().getLocation());
                    debug(last);
                    //temp.getEngine().load(last);
                    load(temp.getEngine(), last);
                }catch(ArrayIndexOutOfBoundsException e){
                    debug("Nothing to go back to!");
                }
            }
        });

        goForward.setOnAction(new EventHandler<ActionEvent>() { //same thing as context menu. Should have made a function.
            @Override
            public void handle(ActionEvent event) {
                WebView temp = browser.get(currentSelection(tabs));

                String forward = tab.get(currentSelection(tabs)).getId();
                if (!forward.equalsIgnoreCase("tab")) {
                    debug(forward);
                    //temp.getEngine().load(forward);
                    load(temp.getEngine(), forward);
                } else {
                    debug("Nothing to go forward to!");
                }
            }
        });

        source.setOnAction(new EventHandler<ActionEvent>() { //set to view source
            @Override
            public void handle(ActionEvent event) {
                viewSource = !viewSource;
                source.setText("View Source: " + viewSource);
                Tab currentTab = tab.get(currentSelection(tabs));
                if(viewSource){
                	//get html and set it as content
                    String code = (String)browser.get(tab.indexOf(currentTab)).getEngine().executeScript("document.documentElement.outerHTML");
                    Label label = new Label(code);
                    ScrollPane codeView = new ScrollPane();
                    codeView.setContent(label); //set the html as content of the scrollpange
                    currentTab.setContent(codeView); //set the scrollpane as content of tab
                }
                else{
                    currentTab.setContent(browser.get(tab.indexOf(currentTab))); //set the content back to the webview
                }
            }
        });

        url.focusedProperty().addListener(new ChangeListener<Boolean>() {//when the text box is selected, select all
            @Override
            public void changed(ObservableValue ov, Boolean t, Boolean t1) {

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (url.isFocused() && !url.getText().isEmpty()) {//url isn't empty, and it's selected
                            url.selectAll();//select all text
                        }
                    }
                });
            }
        });

        scene.heightProperty().addListener(new ChangeListener<Number>() {
            @Override public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight) {
                tabs.setPrefHeight((double)newSceneHeight); //adjust tabs to always be as tall as window.
            }
        });

        go.setOnAction(new EventHandler<ActionEvent>() { //load the url
            @Override
            public void handle(ActionEvent e) {
                String newAddress = url.getText();
                String newAddress_correct = newAddress;
                String finalAddress = "";

                //This fixes the address in the address bar.
                if(!newAddress_correct.contains("http")){
                    newAddress_correct = "http://" + newAddress;
                }
                if(!newAddress_correct.contains("www.")){
                    debug("1");
                    if(newAddress_correct.contains("http://")) {
                        debug("2");
                        newAddress_correct = newAddress_correct.substring(0,6) + "www." + newAddress_correct.substring(7);
                    }
                    else if(newAddress_correct.contains("https://")){
                        debug("3");
                        newAddress_correct = newAddress_correct.substring(0,7) + "www." + newAddress_correct.substring(8);
                    }
                }
                String[] schemes = {"http","https"}; // DEFAULT schemes = "http", "https", "ftp"
                UrlValidator urlValidator = new UrlValidator(schemes);
                if (urlValidator.isValid(newAddress_correct)) { //if valid url, use it
                    debug("valid");
                    finalAddress = newAddress_correct;
                } else { //if not, google it using the google url stub
                    debug("invalid");
                    finalAddress = google + newAddress;
                    debug(finalAddress);
                }
                load(browser.get(tabs.getTabs().indexOf(tabs.getSelectionModel().getSelectedItem())).getEngine(), finalAddress);//load url
                //browser.get(tabs.getTabs().indexOf(tabs.getSelectionModel().getSelectedItem())).getEngine().load(finalAddress);
            }
        });

        url.setOnKeyPressed(new EventHandler<KeyEvent>() //same as go button, but for enter key. Definitely should have made a method.
        {
            @Override
            public void handle(KeyEvent ke)
            {
                if (ke.getCode().equals(KeyCode.ENTER))
                {
                    String newAddress = url.getText();
                    String newAddress_correct = newAddress;
                    String finalAddress = "";
                    debug(newAddress);

                    //This fixes the address in the address bar.
                    if(!newAddress.contains("http")){
                        newAddress_correct = "http://" + newAddress;
                    }
                    if(!newAddress_correct.contains("www.")){
                        debug("1");
                        if(newAddress_correct.contains("http://")) {
                            debug("2");
                            newAddress_correct = newAddress_correct.substring(0,7) + "www." + newAddress_correct.substring(7);
                        }
                        else if(newAddress_correct.contains("https://")){
                            debug("3");
                            newAddress_correct = newAddress_correct.substring(0,8) + "www." + newAddress_correct.substring(8);
                        }
                    }
                    String[] schemes = {"http","https"}; // DEFAULT schemes = "http", "https", "ftp"
                    UrlValidator urlValidator = new UrlValidator(schemes);
                    debug(newAddress_correct);
                    if (urlValidator.isValid(newAddress_correct)) {
                        debug("valid");
                        finalAddress = newAddress_correct;
                    } else {
                        debug("invalid");
                        finalAddress = google + newAddress;
                        debug(finalAddress);
                    }
                    debug("enter");
                    WebView v = browser.get(tabs.getTabs().indexOf(tabs.getSelectionModel().getSelectedItem()));
                   //v.getEngine().load(finalAddress);
                    load(v.getEngine(),finalAddress);
                    debug(v.getEngine().getLocation());
                }
            }
        });

        for(Tab t:tabs.getTabs()){ //keep the tab arraylist and the webview arraylist synchronized. I should have used a hashmap or a class to keep track of this
            t.setOnCloseRequest(new EventHandler<Event>(){
                @Override
                public void handle(Event event){
                    debug("tabs are working");
                    Tab removed = tabs.getSelectionModel().getSelectedItem();
                    tab.remove(tabs.getTabs().indexOf(removed));
                    browser.remove(tabs.getTabs().indexOf(removed));
                }
            });
        }

        browser.get(currentSelection(tabs)).getEngine().getLoadWorker().stateProperty().addListener( //run when url changed
                new ChangeListener<State>() {
                    @Override
                    public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {

                        url.setText(browser.get(currentSelection(tabs)).getEngine().getLocation()); //update url in textbox

                        
                        String address = browser.get(currentSelection(tabs)).getEngine().getLocation();//get address.
                        File file = new File(System.getProperty("user.home") + "/Downloads/"); //get the downloads folder
                        //ArrayList<String> downloadableExtensions = parser.getEndings();
                        String[] downloadableExtensions = {".doc", ".xls",".ppt", ".zip", ".exe",
                                ".rar", ".pdf", ".jar", ".png", ".jpg", ".gif",".mov",".mp4",".wmv",".7z"}; //list of downloadable extension
                        for(String downloadable : downloadableExtensions) {
                            if (address.endsWith(downloadable)) {
                                try {
                                    if(!file.exists()) {
                                        file.mkdir(); //check to make sure downloads folder exists
                                    }
                                    //get file name and create file for it.
                                    String paths[] = address.split("/");
                                    String file_path = paths[paths.length-1];
                                    File download = new File(file + "/" + file_path);
                                    debug(download.getAbsolutePath());
                                    String content = "File already exists!"; //default conent of alert
                                    boolean exists = false;
                                    if(download.exists()) {
                                        exists = true;
                                    }
                                    else {
                                        content = "Download started!"; //set content of alert
                                        URL url = new URL(browser.get(currentSelection(tabs)).getEngine().getLocation()); //get current url
                                        Runnable r = new Download(url,download); //spawn download thread
                                        new Thread(r).start();
                                        //FileUtils.copyURLToFile(new URL(browser.get(currentSelection(tabs)).getEngine().getLocation()), download);
                                    }
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION); //spawn download alert
                                    alert.setTitle("Download");
                                    alert.setHeaderText(null);
                                    alert.setContentText(content);
                                    alert.showAndWait();
                                    if(exists){break;}
                                } catch(Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    }
                }
        );
        tabs.getSelectionModel().selectedItemProperty().addListener(//listening for added tabs
                new ChangeListener<Tab>() {
                    @Override
                    public void changed(ObservableValue<? extends Tab> ov, Tab t, Tab t1) {
                        if (tabs.getSelectionModel().getSelectedItem().getId().equals("+")) {//if selected tab is 'add' tab
                            Tab temp = new Tab(); //create new tab
                            WebView browser_temp = new WebView(); //new webview
                            temp.setText("Tab"); //set title text and id.
                            temp.setId("tab");
                            tabs.getTabs().add(tabs.getTabs().size()-1,temp); //add tab to tabpane
                            temp.setContent(browser_temp); //set content of tab
                            //browser_temp.getEngine().load(toGo);
                            load(browser_temp.getEngine(), toGo); //load up url
                            browser.add(browser.size(),browser_temp);//add webview to arraylist
                            temp.setClosable(true);
                            temp.setOnCloseRequest(new EventHandler<Event>() {//add close listener
                                @Override
                                public void handle(Event event) {
                                    debug("tabs are working");
                                    Tab removed = tabs.getSelectionModel().getSelectedItem();
                                    tab.remove(tabs.getTabs().indexOf(removed));
                                    browser.remove(tabs.getTabs().indexOf(removed));
                                }
                            });
                            tab.add(tab.size(),temp);//add tab to tab arraylist
                            SingleSelectionModel<Tab> selectionModel = tabs.getSelectionModel();
                            selectionModel.select(temp); //focus on new tab
                        } else {
                            url.setText(browser.get(currentSelection(tabs)).getEngine().getLocation()); //set the urlbox to the url of the selected tab's webview
                        }
                    }
                }
        );


        url_layout.getChildren().addAll(goBack,goForward,reloadButton,url, go,hyper,source);//add UI to layout
        url_layout.setSpacing(10); //add UI spacing

        root.getChildren().addAll(url_layout,tabs);//add layouts to root layout
        scene.setRoot(root); //add root to scene
        stage.setScene(scene); //add scene to stage
        stage.show(); //show stage
        showHyperDodgeDialog(); //show info dialog about proxy
    }
    private class Download implements Runnable{
    	private URL url;
    	private File file;
    	public Download(URL url, File file){
    		this.url = url;
    		this.file = file;
    	}
    	
    	public void run(){
    		try{
    			FileUtils.copyURLToFile(url, file); //using Apache.FileUtils. I definitely could have done this on my own. why???
    		}
    		catch(Exception e){
    			e.printStackTrace();
    		}
    	}
    }
    
    static void debug(Object println){if(debug) System.out.println(println);} //print debug statement

    static int currentSelection(TabPane tabs){
        return tabs.getSelectionModel().getSelectedIndex(); //return the index of the current selection
    }
    
    private static boolean useSocket = false; //use socket system? default is false, so will use proxy. (proxy is more reliable and faster and better)
    private static void load(WebEngine engine, String url){
    	try {
    		if(!useSocket) throw new Exception(); //check for if using socket system or proxy.
			PrintWriter out = new PrintWriter(socket.getOutputStream()); //get outputstream
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); //get inputstream
			out.println(url); //send the url to the server
			out.flush();
			String s;
			StringBuilder html = new StringBuilder();
			while((s = in.readLine()).charAt(0) != Proxy.END){ //read until the server send sthe stop signal
				html.append(s); //append what is read to the html
			}
			engine.loadContent(html.toString()); //load the html in webview
			debug("Proxied!");
		} catch (Exception e) {
			//e.printStackTrace();
			engine.load(url); //load url. proxy will automatically be used.
		}
    }
    
    private static void showHyperDodgeDialog(){ //show info dialog about proxy.
    	Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Dodge Browser");
		alert.setHeaderText(null);
		alert.setContentText("HyperDodge allows you to bypass all web filtering.\n"
				+ "However, performance may be significantly impacted.\n"
				+ "You can disable it at the top of the window.");
		alert.showAndWait();
    }
    private void setUpSocket(){ //set up socket for socket system. Unused right now.
    	try{
    		socket = new Socket(SERVER, PORT);
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}
    }
    public static void main(String[] args) {
    	//only allow this to be run on school computers. program will only launch on pre-approved computers, based on the username.
        String[] allowed = new String[]{"s-deshpandea","s-koniga","s-sumpterg","s-vemurik","s-wangy","s-andersoni","s-andersonjos"};
        String user = System.getProperty("user.name");
        boolean approved = false;
        for(String index : allowed){
            approved = user.equals(index); //is this user allowed to use the users
            if(approved) break;
        }
        if(approved || true){ //the '|| true' is ONLY added so this code can be run anywhere for github. In releases, this || true should NOT be present.
        	//setUpSocket();
        	System.setProperty("http.proxyHost", SERVER);//set the proxies
        	System.setProperty("http.proxyPort", String.valueOf(PORT));
        	System.setProperty("https.proxyHost", SERVER);
        	System.setProperty("https.proxyPort", String.valueOf(PORT));
        	debug(System.getProperty("http.proxyHost") + ":" + System.getProperty("http.proxyPort"));
            launch(args); //launch javafx application
        }
        else{
            System.exit(0);
        }
    }
}