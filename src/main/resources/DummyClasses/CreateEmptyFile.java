import java.io.File;
import java.io.IOException;

class CreateEmptyFile {
  public static void main(String[] args) {
    var fileName = "emptyFile.txt";
    var file = new File(fileName);

    try {
      if (file.createNewFile()) {
        System.out.println("File created: " + file.getName());
      } else {
        System.out.println("File already exists.");
      }
    } catch (IOException e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
  }
}
