/*
DropReName
2015/12/30 by halbmond.die.zweite@gmail.com

[ このアプリを作った目的 ]
南相馬市での除染作業終了時、
工事写真を元請け会社に提出するのですが、
各作業工程で撮ったJPGファイルの名前を
作業内容に合わせてリネームしなければなりません。
ひと現場あたり50枚前後提出します。
ひと月で200〜300枚ぐらい。

コピペや、辞書登録による手打ちでの変更が
とても手間だったため、
マウスによるドラッグ・ドロップだけで
リネームできれば楽だなと思い、
書籍やネットで調べ物しながら作りました。

自分自身、仕事で使って重宝しています。
別の会社の管理者が写真整理で四苦八苦していたので
試しに使ってもらっています。
彼があまりにパソコンに疎いので、
Windows PC にJavaをインストールしてあげて、
Pathを設定し、アプリを起動し、
使い方を教えてあげました。
ちなみにその会社の人妻事務員さんが
僕のタイプの女性だったので、
未完成ながらもアプリ制作のやりがいはありました。


[ このアプリの本質的な機能 ]
1.ファイルをNameListで指定した文字列でリネームする
2.重複するファイル名にはナンバリングする。

[ このアプリの使い方 ]
このアプリのリストセルにファイルをドロップすると
そのリストセルの文字列にリネームする。
複数ある場合は連番でナンバリングする。

ソースコードは単一ファイルのスパゲティ状態。
勉強しながらリファクリングをしたい。


【このアプリの補助的な機能】
オプションとして設定
・空白文字列を挿入
・アンダーバーを挿入
・ナンバリングの数字を指定記号で囲む
　ファイル名に使える文字列を調べる
　例　ファイル名(1).jpg
・不要フォルダへ移動

*/


import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.Cursor;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.SelectionMode;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import javafx.scene.input.TransferMode;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.File;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import java.util.List;
import java.util.ArrayList;
import javafx.util.Callback;
import java.nio.file.DirectoryStream;


public class DropReName6 extends Application{
	
	TextField inputRoadNumberField = new TextField();
	public String roadNumber;

	ListView<String> listView = new ListView<>();

	String bottomPanelMassage = "不要_路線番号 フォルダへ移動";
	Label bottomPanel = new Label(bottomPanelMassage);


	@Override
	public void start(Stage stage) throws Exception{
		stage.setTitle("DropReName Ver0.5   2015/11/25");
		stage.setWidth(440);
		//stage.setHeight(700);
		
		
		//topPanel 路線番号を入力。
		HBox topPanel = new HBox();
		topPanel.setPrefHeight(40);
		
		Label roadNumberLabel = new Label("路線番号 ->");
		roadNumberLabel.setPrefHeight(40);
		roadNumberLabel.setPrefWidth(120);
		roadNumberLabel.setFont(Font.font("Verdana", 18));
		inputRoadNumberField.setPrefWidth(160);
		inputRoadNumberField.setFont(Font.font("Verdana", 18));
		
		Button inputButton = new Button("入力");
		inputButton.setFont(Font.font("Verdana", 18));
      inputButton.setOnMouseClicked(event -> updateValue(stage));
		inputButton.setCursor(Cursor.HAND);
		
		topPanel.getChildren().addAll(
		                				  roadNumberLabel,
				          				  inputRoadNumberField,
											  inputButton);
		
		//centerPanel ListView
		//リストを選択できないようにはできるか？
		listView.setEditable(false);
		//listView.setPrefWidth(360);
		listView.setPrefHeight(550);
		final IntegerProperty dragFromIndex = new SimpleIntegerProperty(-1);
		listView.setCellFactory(new Callback<ListView<String>, ListCell<String>>(){
			@Override
			public ListCell<String> call(ListView<String> lv){

		
			final ListCell<String> cell = new ListCell<String>() {
			                    public void updateItem(String item, boolean empty) {
			                        super.updateItem(item,  empty);
			                        if (empty) {
			                            setText(null);
			                        } else {
			                            setText(item);
			                        }
			                    }
			                };
         cell.setFont(Font.font("Verdana", 15));
			cell.setOnDragOver(event -> objectDragOver(event));
			cell.setOnDragEntered(new EventHandler<DragEvent>() {
			    @Override
			    public void handle(DragEvent event) {
						    Dragboard db = event.getDragboard();
                       cell.setStyle("-fx-background-color: pink;");
			                       			       }
			                });
			
			  // remove highlight:
			 //ドラッグされたオブジェクトがコントロールの領域から離れた。
			 cell.setOnDragExited(new EventHandler<DragEvent>() {
			    @Override
			    public void handle(DragEvent event) {
			      cell.setStyle("");
			    }
			 });

         cell.setOnDragDropped(event -> dropReName(event, cell.getText()));
			
			return cell;
			}
			
					
		});





		//bottomPanel 不要フォルダーパネル
		bottomPanel.setPrefHeight(100);
		bottomPanel.setPrefWidth(350);
		bottomPanel.setFont(Font.font(16));
		bottomPanel.setOnDragOver(event -> objectDragOver(event));
		bottomPanel.setOnDragDropped(event -> removeToUnnecessaryFolder(event));
		
		
		//DropReNameのルートノードはVBoxでは不適切
		VBox rootBox = new VBox();
		rootBox.setAlignment(Pos.TOP_CENTER);
		rootBox.setPadding(new Insets(5, 5, 5, 5));
		rootBox.setSpacing(5.0);
		rootBox.getChildren().addAll(topPanel, listView, bottomPanel);
		
		
		stage.setScene(new Scene(rootBox));
		stage.setAlwaysOnTop(true);
		stage.show();
	}



	//クリックしたらroadNumberを代入するメソッド。　　　　未完
	void updateValue(Stage stage)
	{
		/*taskList.setText(roadNumber);
		各リストに路線番号を代入
		*/
		roadNumber = inputRoadNumberField.getText();
		//この処理はStringBufferを使うと速いらしい?
		bottomPanel.setText(bottomPanelMassage.replace("路線番号",roadNumber));
		readTaskList(roadNumber);
		
		inputRoadNumberField.setText("");
		//nullにするとエラー処理を書かなきゃいけない？
		
		//buttonを消す処理はどう書けばいいの？
	}

	
	//ファイルをドラッグしてコントロールの領域に入ったときの処理。
void objectDragOver(DragEvent event){
	Dragboard db = event.getDragboard();
  		if (db.hasFiles()) {
          event.acceptTransferModes(TransferMode.COPY);
          //カーソルのある場所の色を反転させたい。
      }
          event.consume();
}

	
	//路線番号をもとにTaskListを作成するメソッド
	public void readTaskList(String inputNumber){
      final String nameListFile = "NameList.txt";
      String roadNumber = inputNumber;
	   List<String> taskList = new ArrayList<>();
		try (Stream<String> stream = Files.lines(Paths.get(nameListFile), 
		                                       StandardCharsets.UTF_8))
		{
			stream.forEach(taskName -> {
				//System.out.println(taskName.replace("路線番号", inputNumber));
				taskList.add(taskName.replace("路線番号", inputNumber));
			});
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(nameListFile + " ファイルが存在していません。");
		}
			ObservableList<String> names = FXCollections.
			                               observableArrayList(taskList);
			listView.setItems(names);
	}





   //リネームするメソッド
	void dropReName(DragEvent event, String cellText){
		Dragboard db = event.getDragboard();
		FileSystem fs = FileSystems.getDefault();
		String taskName = cellText;
		boolean success = false;
		if (db.hasFiles()){
			success = true;
			for (File dropedFiles : db.getFiles()) {
				Path dropedFilesPath = dropedFiles.toPath();


				findSamePath(dropedFilesPath, taskName);


				try {
                  if(Files.exists(fs.getPath(taskName))){
						    System.out.println("同じ名前のファイルが存在しているので番号付けします。");
						    
					   } else {
    
					   }
					
				      } catch (Exception e) {
					   e.printStackTrace();
				   }
			   }
			}
		   event.setDropCompleted(success);
		   event.consume();
	   }


//ReName classが必要かな。
//同名ファイルを探す
//pathと文字列を送ると、同じ名前を持つファイルの数を返すメソッド
      void findSamePath(Path originalPath, String sameName){
        System.out.println("親フォルダのパス : " + originalPath);
        Path oldFileName = originalPath.getFileName();
        int pathsCount = 0;
        
        //Streamのフィルターを使用してsameNameを持つファイルのみを処理の対象とする
        try (DirectoryStream<Path> pathsStream = 
             Files.newDirectoryStream(originalPath.getParent(), sameName + "*.*")) {
                //フィルターを通してsameNameと一致するファイルを数える。
                for (Path path : pathsStream) {
                    pathsCount++;
                    //System.out.println(path.getFileName());
                    
/*
                   System.out.println("オリジナルのファイル名 : " + 
                                      path.getFileName().toString());

                   System.out.println("変更し、クリップした名前 : " +
                                      sameName +
                                      clipInParenthesis(pathsCount++)); 
                   //tureならこの名前を使う
                   System.out.println("結果 : " + 
                                      path.getFileName().
                                      toString().
                                      startsWith(sameName) + "だよ\n");
*/
                }
                //この数をもとにファイル名を連番にする。
                System.out.println("与えられたtaskNameと一致するファイルの数 : " + pathsCount + "個");
                System.out.println("変更前 : " + oldFileName);
                StringBuilder newFileName = new StringBuilder();
                newFileName.append(sameName);
                newFileName.append(clipInParenthesis(pathsCount + 1));
                newFileName.append(getFileNameExtension(oldFileName)).toString();
                System.out.println("変更後 : " + newFileName);
                
                Path newFileNamePath = Paths.get(originalPath.getParent().toString(), newFileName.toString());
                Files.move(originalPath, oldFileName.resolveSibling(newFileNamePath));
                System.out.println("行き先 : " + newFileNamePath);
              }catch (Exception e) {
    
              }
        }


//文字列の末尾から拡張子を取得するメソッド　
public String getFileNameExtension(Path path){
    
   String fileName = path.getFileName().toString();
   int lastDotPosition = fileName.lastIndexOf(".");

   if (lastDotPosition != -1){
      return fileName = fileName.substring(lastDotPosition);
   }
   return null;
}



//文字列の末尾に拡張子を追加するメソッド


//受け取った数字を()で挟んで文字列として返すメソッド。<-ClipInとしてtools化済み
public String clipInParenthesis (int number){
   StringBuilder clipper = new StringBuilder();
   String parenthesis = " ()";
   clipper.append(parenthesis);
   clipper.insert(2, number);

   return clipper.toString();
}




	//ドロップしたオブジェクトを不要フォルダへ移動するメソッド
		void removeToUnnecessaryFolder(DragEvent event){

			Dragboard db = event.getDragboard();
			FileSystem fs = FileSystems.getDefault();
         String folderName = "不要_" + roadNumber;
			boolean success = false;

			if (db.hasFiles()) {
		      success = true;

		      for (File dropedFiles : db.getFiles()) {
			       //FileオブジェクトからPathオブジェクトに変換
			       Path dropedFilesPath = dropedFiles.toPath();
                Path unnecessaryFolder = fs.getPath(
                                                    dropedFilesPath.
                                                    getParent().
                                                    toString(), folderName);
                try {
	                  if (Files.exists(unnecessaryFolder)) {
		                   Files.move(dropedFilesPath, 
                                    unnecessaryFolder.
                                       resolve(dropedFilesPath.getFileName())
                                    );
	                     } else {
                             Files.createDirectory(unnecessaryFolder);
                             Files.move(dropedFilesPath, 
                                        unnecessaryFolder.
                                           resolve(dropedFilesPath.getFileName()));
                        }
                     } catch (IOException e) {
                         //e.printStackTrace();
	                      System.out.println("同名ファイル問題を解決するメソッドが必要です。");
	                      System.out.println("アラートウィンドウの表示を実装すること。");
		                   System.out.println("もしくは同名ファイルが存在した時の衝突回避を実装すること");

                     }
            }
            event.setDropCompleted(success);
            event.consume();
		   }
	    }	

}
