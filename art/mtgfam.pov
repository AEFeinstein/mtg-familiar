#version 3.7;
global_settings { assumed_gamma 1.0 }
 
#include "colors.inc"
#include "shapes.inc"
#include "SuperCone.inc"

camera {
    orthographic
    location <0, 2.2, 0>
    angle 0
    right x*image_width/image_height
    look_at <0,0,0>
}

// Uncomment the area lights only if you've got lots of time.
#declare Dist=80.0;

light_source {
    < 0, 250, 0>
    color White
     fade_distance Dist
     fade_power 2
   adaptive 1
   jitter
}


light_source {
    < -2, 1, 2>,
    color White
//  fade_distance Dist
//  fade_power 2
//  area_light <-40, 0, -40>, <40, 0, 40>, 3, 3
//  adaptive 1
//  jitter
    parallel
    point_at <0,0,0>
}

light_source {
    < 0, 0, 2>,
    color White
//  fade_distance Dist
//  fade_power 2
//  area_light <-40, 0, -40>, <40, 0, 40>, 3, 3
//  adaptive 1
//  jitter
    parallel
    point_at <0,0,0>
}

#declare mtg_ColorArray = array[5];
#declare mtg_ColorArray[0] = rgb <224/256, 224/256, 224/256>;
#declare mtg_ColorArray[1] = rgb <25/256 , 118/256, 210/256>;
#declare mtg_ColorArray[2] = rgb <97/256 , 97/256 , 97/256>;
#declare mtg_ColorArray[3] = rgb <244/256, 67/256 , 54/256>;
#declare mtg_ColorArray[4] = rgb <76/256 , 175/256, 80/256>;
#declare base_color = rgb <250/255,250/255,250/255> / (5);

#declare tRadius = 0.5;
#declare triangleVertices = array[3];
#declare triangleVertices[0] = <tRadius * sin( 0.0 * pi / 5.0), 0.0, tRadius * cos( 0.0 * pi / 5.0)>;
#declare triangleVertices[1] = <tRadius * sin( 3.0 * pi / 5.0), 0.0, tRadius * cos( 3.0 * pi / 5.0)>;
#declare triangleVertices[2] = <tRadius * sin(-3.0 * pi / 5.0), 0.0, tRadius * cos(-3.0 * pi / 5.0)>;

#declare cornerRadius = 0.05;

#declare transAmt = 0.05;
#declare wedgeHeight = 0.25;

union{
    #declare I = 0;
    #while (I < 5)
        union {
 
            // Draw some triangles
            #declare J = 0;
            #while (J < 1)
                object {
                    triangle {
                        triangleVertices[0],
                        triangleVertices[1],
                        triangleVertices[2]
                        texture {
                            pigment {
                                gradient y
                                color_map {
                                    [0  color mtg_ColorArray[I]]
                                }
                            }
                            finish{ambient 1}
                        }
                        translate <0, wedgeHeight * J, 0>
                    }
                }
                #declare J = J + 1;
            #end
 
 
            // Draw some cylinders for corners
            #declare J = 0;
            #while (J < 3)
                object {
                    cylinder {
                        <0,0,0>
                        <0, wedgeHeight, 0>,
                        cornerRadius
                        texture {
                            pigment {
                                gradient y
                                color_map {
                                    [0  color mtg_ColorArray[I]]
                                }
                            }
                            finish{ambient 1}
                        }
                        
                        #declare a =  triangleVertices[mod(J + 1, 3)] - triangleVertices[J];
                        #declare b =  triangleVertices[mod(J + 2, 3)] - triangleVertices[J];
                        #declare angleBetween = acos(vdot(a,b) / (vlength(a) * vlength(b)));
 
                        // First move the cylinder inward, along one edge
                        translate vnormalize(b) * cornerRadius / sin(angleBetween/2)
                        // Then rotate it into place
                        rotate <0, -90 * angleBetween / pi, 0>
                        // Then move it to the vertex
                        translate triangleVertices[J]
                    }

                }
            #declare J = J + 1;
            #end
 
            // Draw some walls
            #declare J = 0;
            #while (J < 3)
                object {
                    polygon {
                        5,
                        (triangleVertices[mod(J, 3)]),
                        (triangleVertices[mod(J, 3)]     + <0, wedgeHeight, 0>),
                        (triangleVertices[mod(J + 1, 3)] + <0, wedgeHeight, 0>),
                        (triangleVertices[mod(J + 1, 3)]),
                        (triangleVertices[mod(J, 3)])
                        texture {
                            pigment {
                                gradient y
                                color_map {
                                    [0  color mtg_ColorArray[I]]
                                }
                            }
                            finish{ambient 1}
                        }
                    }
                }
                #declare J = J + 1;
            #end
            
            // Translate and rotate the whole wedge
            translate <0, 0, -(transAmt + tRadius)>
            rotate <0, (360 / 5) * I, 0>
        }
        #declare I = I + 1;
    #end

    object {
        // Draw the base
        Supercone (
            <0,0,0>,
            (tRadius) * 2, (tRadius) * 2 * 0.95,
            <0,-wedgeHeight, 0>,
            (tRadius) * 2, (tRadius) * 2)
        texture {
            pigment {
                gradient y
                color_map {
                    [0  color base_color]
                }
            }
            finish{ambient 1}
        }
        rotate <0, -36, 0>
    }
    
    // Rotate everything to the correct orientation
    rotate <0, 216, 0>
}
