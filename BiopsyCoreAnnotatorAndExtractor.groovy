// **********************INPUT PARAMETERS**********************
int biopsyCoreLength_microns = 2500;  //i.e. 2.50 mm
int biopsyCoreWidth_microns  = 250;	//i.e. 0.25 mm
//*************************************************************

// boilerplate
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane

// Get image resolution
def serverp = getCurrentServer()
def calibration = serverp.getPixelCalibration()
float resolution_micronsPerPixel = calibration.getPixelWidthMicrons()
print resolution_micronsPerPixel

// Get biopsy core length and width in pixels
int biopsyCoreLength_pixels = biopsyCoreLength_microns/resolution_micronsPerPixel
int biopsyCoreWidth_pixels = biopsyCoreWidth_microns/resolution_micronsPerPixel
 
 //=============================================== DRAW ANNOTATIONS ===================================================================================================================================
def imageData = getCurrentImageData()
def imageInfoServer = imageData.getServer()
def imageHeight = imageInfoServer.getHeight();

def plane = getCurrentViewer().getImagePlane()
def AllBiopsyCoreAnnotations = []

// The origin (the top left corner) of first core should start at zero. 
// This value is incremented by the biopsyCoreLengthInPixels.
// This is until the end of the image is reached. 
// "imageHeight - biopsyCoreLength_pixels" is used because otherwise we'd have a core starting
// at the end of the image which means that it would end outside the image (which is not possible and would error) 
for (int y_coordinateOfBiopsyCoreOrigin = 0; y_coordinateOfBiopsyCoreOrigin <  imageHeight - biopsyCoreLength_pixels; y_coordinateOfBiopsyCoreOrigin += biopsyCoreLength_pixels) {
    
	// See notes above y-loop, as we're doing the same thing as that but in the x-direction
	for (int x_coordinateOfBiopsyCoreOrigin = 0; x_coordinateOfBiopsyCoreOrigin < imageInfoServer.getWidth() -  biopsyCoreWidth_pixels; x_coordinateOfBiopsyCoreOrigin += biopsyCoreWidth_pixels) {
		
		// Create annotation object for one biopsy core using the current coordinates
        def biopsyCoreAnnotation = ROIs.createRectangleROI(x_coordinateOfBiopsyCoreOrigin, y_coordinateOfBiopsyCoreOrigin, biopsyCoreWidth_pixels, biopsyCoreLength_pixels, plane)
        
		// Add current annotation object to list of annotation objects we want to draw on this image
		AllBiopsyCoreAnnotations << PathObjects.createAnnotationObject(biopsyCoreAnnotation)
    }
}
// Draw the annotations on the slide based on the list of objects
addObjects(AllBiopsyCoreAnnotations)

//=============================================== WRITE BIOPSY CORE IMAGES ===================================================================================================================================
// Create directory for biopsy cores
def slideName = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
def pathOutput = buildFilePath(PROJECT_BASE_DIR, 'biopsyCores', slideName)
mkdirs(pathOutput)

// Export the biopsy cores
new TileExporter(imageData)
    .imageExtension('.tif')
    .tileSize(biopsyCoreWidth_pixels, biopsyCoreLength_pixels) // the dimensions in pixels are what's expected
    .annotatedTilesOnly(false) 
    .writeTiles(pathOutput)                                    // Write tiles to the specified directory

print('Done')