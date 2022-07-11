import java.io.BufferedReader;
import java.io.FileReader;
import qupath.lib.objects.PathAnnotationObject;
import qupath.lib.roi.RectangleROI;

def imageData = getCurrentImageData();
def imageName = imageData.getServer().getMetadata().getName();

def file = new File("D:/Users/sdammak/QuPath Project Validation Only/v2/Model Output/002 Experiment Output/"+imageName[0..-5]+"_predictions.csv")
print file

def csvReader = new BufferedReader(new FileReader(file))

int sizePixels = 1000
row = csvReader.readLine()

while ((row = csvReader.readLine()) != null) {
    def rowContent = row.split(",")
    double cx = rowContent[1] as double;
    double cy = rowContent[2] as double;
    double l = 224 as double;
    def roi = new RectangleROI(cx,cy,l,l);
    def annotation = new PathAnnotationObject(roi, PathClassFactory.getPathClass("Dysplasia_test"));
    imageData.getHierarchy().addPathObject(annotation, true);
}
// merge annotations



