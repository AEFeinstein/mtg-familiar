// POV-Ray 3.6 include file "Supercone.inc"                                                            
// author: Friedrich A, Lohmueller, Febr-2008
// homepage: http://www.f-lohmueller.de/
// email: Friedrich.Lohmueller_aT_t-online.de
//----------------------------------------------------
#ifndef( Supercone_Inc_Temp)
#declare Supercone_Inc_Temp = version;
#version 3.5;
//----------------------------------------------------
#ifndef(  Transforms_Inc_Temp)
#include "transforms.inc"                                             
#end      
//------------------------------------------------------------------------------ /////////  
#macro Supercone(PtA, A, B, PtB, C, D) // corrected macro by Warp 20.10.2001
   intersection {
      quartic {
         <0, 0,  0,  0,  0,  0,  0,  B*B-2*B*D+D*D, 2*(B*D-B*B), B*B,
         0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
         0,  0,  0, A*A-2*A*C+C*C, 2*(A*C-A*A), A*A, 0,  0,  0,  0,
         -(A*A-2*A*C+C*C)*(B*B-2*B*D+D*D),
         -(2*((B*D-B*B)*(A*A-2*A*C+C*C)+(A*C-A*A)*(B*B-2*B*D+D*D))),
         -(B*B*(A*A-2*A*C+C*C)+4*(A*C-A*A)*(B*D-B*B)+A*A*(B*B-2*B*D+D*D)),
         -(2*(B*B*(A*C-A*A)+A*A*(B*D-B*B))), -A*A*B*B>
         sturm
      }
      cylinder {0, z, max(max(abs(A), abs(B)), max(abs(C), abs(D)))}
      
     //bounded_by {cone {0, max(abs(A)*3, abs(B))*3, z, max(abs(C), abs(D))*3}}   // changed by F.A.Lohmueller 24-Febr-2008
      
      #local Dirv = PtB - PtA;
      scale <1,1,vlength(Dirv)>
      #local Dirv = vnormalize(Dirv);
      #if(vlength(Dirv-<0,0,-1>)=0) scale <1,1,-1>
      #else Reorient_Trans(z, Dirv)
      #end
      translate PtA
   }
#end// of macro ------------------------------------------------------// end of macro

#version Supercone_Inc_Temp;
#end
//--------------------------------------------------------------------// end of include file

