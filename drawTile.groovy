def plane = getCurrentViewer().getImagePlane()
def biopsyCoreAnnotation = ROIs.createRectangleROI(25088, 38304, 224,224, plane)

addObjects(PathObjects.createAnnotationObject(biopsyCoreAnnotation))
print('Done!')