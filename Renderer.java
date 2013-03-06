package computing_mathematic;

import javax.media.opengl.GL;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;

import javax.swing.JOptionPane;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;


public class Renderer 
implements GLEventListener, KeyListener, MouseListener, MouseMotionListener {
  
   private static final GLU glu = new GLU();	//создание нового объескта библиотеки OpenGL
   private final int max_side=200;				// максимальное количество точек в ширину(длину)
   public double   XstepX = 2.0d,	// расстояние между частицами 
				   h=0.0225d,		// шаг интегрирования
				   c = 0.5d,		// константа демпфирования
				   eps=0.001d;		// точность вычиления dv
   public int sizeXxY = 5,				// количестов частиц в ширину(длину)
   			  start = 0,				// селектор действий
   			  num_iteration=100,		// максимальное количество итераций
   			  static_points=3,			// количество закрепленных точек
   			  pause=0;
   public double mass = 40.0d,										 // масса ткани в гр
				 mass1=mass/(sizeXxY*sizeXxY),						 // масса одной частицы
				 ks = 1.375d, kb =0.0d/*1.09d*/, kt=0.0d/*1.425d*/ ;				 // коэффициенты 
   private double movE_X = 29.0d, movE_Y = 12.0d,  movE_Z = 17.0d,   // положение точки с которой смотрит камера
				  stepX = XstepX, 				// шаг между частицами по оси X
				  stepY = XstepX, 				// шаг между частицами по оси Y
				  stepZ = 0.01d,				// шаг между частицами по оси Z	
   				  slowMotionRatio = 1000, 
			      timeElapsed = 0, 
			      milliseconds, dt,
   				  grav_free_fall = 9.8d,		// усокрение свободного падения м/с
		   P[][][]= new double[max_side][max_side][3], 		// массив положений частиц
		   f[][][] = new double[max_side][max_side][3],		// массив сил действующих на частицу
		   v[][][] = new double[max_side][max_side][3],		// массив скоростей частиц
		   vRK[][][] = new double[max_side][max_side][3],
		   vSumm[][][] = new double[sizeXxY][sizeXxY][3],
		   Ptmp[][][]= new double[max_side][max_side][3],
   		   		 CSR_elem[][];				// не нулевые значения матрицы А будут храниться здесь 
   private int   CSR_iptr[],				//
   	             CSR_jptr[],				//
   	             CSR_size=0,				// размер матрицы CSR 
   	             mouse_on_scene = 0, 		// курсор в области моделирования ?
   	             koef=0;					// переключатель коэффициентов взаимодействий


   
   
   public void display(GLAutoDrawable gLDrawable) { 
	   stepX = XstepX; stepY = XstepX; // для динамического изменения параметров
	   
	   final GL gl = gLDrawable.getGL();
       
       gl.glClear(GL.GL_COLOR_BUFFER_BIT);
       gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
       gl.glLoadIdentity();

       // положение "камеры"
       glu.gluLookAt( movE_X, movE_Y, movE_Z ,
		   		  0.0d, 0.0d-2, 0.0d+2,
		   		  0.0d, 1.0d, 0.0d );
    
       // прорисовка уровня земли
       //doGridZero(gl);
       // задание координат частиц
       if (start == 0) doGridPoints(); 
       // отображнеие полотна
       doShitShow(gl);
       // падение ткани по явному Эйлеру
       if(start == 1) doShitFall(gl);
       // падение ткани по не Явному Эйлеру
       if(start == 2) doShitFall2(gl);
       // падение ткани по не РК4
       if(start == 3) {doShitFall3(gl);
				       /*try {
							System.in.read();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}*/
       					}
       if(start == 4) {//doOption(gl);
       					start =0;}
       
       if(pause==1) {	
						}
      
   }
   
   private void doCSR_Check(GL gl) {
	   
   }


	private void doOption( double[][][] mat, double[][][] vec) {
		
		System.out.println("doOption");
		
	}


   /*================================================================*/
   /*Падение ткани. Обратный метод Эйлера*/
   /*================================================================*/ 
   private void doShitFall2(GL gl) {
	   double dv[][][] = new double[sizeXxY][sizeXxY][3];
	   long milis = System.currentTimeMillis();
		
	   milliseconds = milis;
	   dt = milliseconds / 1000.0f; 	  // Преобразуем миллисекунды в секунды
	   dt /= slowMotionRatio;             // Делим на slowMotionRatio
	   timeElapsed += dt;                 // Изменяем кол-во прошедшего времени
	   	     
	   //внешние силы
	   for(int i=0; i<sizeXxY; i++){ 				
		   for(int j=0; j<sizeXxY; j++){				
	           f[i][j][0] =(-1)* c * v[i][j][0];
	           f[i][j][1] =(-1)* c * v[i][j][1];	
	           f[i][j][2] =(-1)* c * v[i][j][2] - grav_free_fall*mass1 ;
	       }
	   }
	      
	   double len;
	   //внутренние силы
	   for(int i=0; i<sizeXxY; i++){ 				
		   for(int j=0; j<sizeXxY; j++){	
			   for(int k=-1; k<=1; k++){
		            if (i+k<0 || i+k>=sizeXxY) continue;
		            for(int l=-1; l<=1; l++){
		                if (j+l<0 || j+l>=sizeXxY) continue;
		                len=getDistance(P[i][j][0],P[i][j][1],P[i][j][2],P[i+k][j+l][0],P[i+k][j+l][1],P[i+k][j+l][2]);                                                                          
		                for(int m=0; m<3; m++){
		                    if (Math.abs(k)+Math.abs(l)==1){f[i][j][m] += addInnerForce(P[i][j][m],P[i+k][j+l][m],ks*100,XstepX,len);}
		                    if (Math.abs(k)+Math.abs(l)==2){f[i][j][m] += addInnerForce(P[i][j][m],P[i+k][j+l][m],kb*100,Math.sqrt(2*XstepX*XstepX),len);}
		                }
		            }
		        }
			   for(int k=-2; k<=2; k+=2){
		            if (i+k<0 || i+k>=sizeXxY) continue;
		            for(int l=-2; l<=2; l+=2){
		                if (j+l<0 || j+l>=sizeXxY) continue;
		                len=getDistance(P[i][j][0],P[i][j][1],P[i][j][2],P[i+k][j+l][0],P[i+k][j+l][1],P[i+k][j+l][2]);    
		                for(int m=0; m<3; m++){
		                	if (Math.abs(k)+Math.abs(l)==2){f[i][j][m] += addInnerForce(P[i][j][m],P[i+k][j+l][m],kt*100,2*XstepX,len);}
		                }
		            }
	           }
		   }
	   }
	   
	   dv=Search_dv();
	   
	   	   
	   //новое положение
	   for(int i=0; i<sizeXxY; i++){
		   for(int j=0; j<sizeXxY; j++){
			   if(static_points==0){}
			   if(static_points==1){if(i==0 && j==0) continue;}
			   if(static_points==2){if(i==0 && j==0) continue;
								if(i==0 && j==sizeXxY-1) continue;}
				if(static_points==3){if(i==0 && j==0) continue;
								if(i==0 && j==sizeXxY-1) continue;
								if(i==0 && j==sizeXxY/2) continue;}
				if(static_points==4){if(i==0 && j==0) continue;
								if(i==0 && j==sizeXxY-1) continue;
								if(i==0 && j==sizeXxY/3) continue;
								if(i==0 && j==2*sizeXxY/3) continue;
								}
				if(static_points==5){if(i==0 && j==0) continue;
								if(i==0 && j==sizeXxY-1) continue;
								if(i==0 && j==sizeXxY/4) continue;
								if(i==0 && j==2*sizeXxY/4) continue;
								if(i==0 && j==3*sizeXxY/4) continue;}
	           for(int k=0; k<3; k++){
	               P[i][j][k] += h * (v[i][j][k]+dv[i][j][k]);
	               //System.out.print("" + P[i][j][k] +" ");
	           }//System.out.println("");
	       }
	   }   
	   
	   
	 //скорости
	   for(int i=0; i<sizeXxY; i++) {
		   for(int j=0; j<sizeXxY; j++){
			   for(int k=0; k<3; k++){
	               v[i][j][k] +=dv[i][j][k];
			       //System.out.print("" + v[i][j][k] +" ");
			   }//System.out.println("");
	       }
	   }
	   
	   
}

	private double[][][] Search_dv() {
		int N=(int) Math.pow(sizeXxY, 2);
		double dfr[][][] = new double[N][N][9],
			  dfrXv[][][] = new double[sizeXxY][sizeXxY][3],
			  A[][][] = new double[N][N][9],
			  b[][][] = new double[sizeXxY][sizeXxY][3];
		double dv[][][] = new double[sizeXxY][sizeXxY][3];
		
		dfr=dF_r();
		
		System.out.println("\n ");
			for(int i=0; i<N; i++){
				   for(int j=0; j<N; j++){
					   	for(int k=0; k<9; k++){
					   		A[i][j][k]= (getMass(i,j,k)-h*dF_v(i,j,k)-Math.pow(h, 2)*dfr[i][j][k]);
					   		//if(i==0 && j==1)
					   			//System.out.print(A[i][j][k] +"=A["+i+"]["+j+"]["+k+"] ");
					   	}//System.out.println(" ");
				   }
			 }
			
			dfrXv=MatrixMltplVector(dfr,v);
			
			/*System.out.println(" ");
			 for(int i=0; i<sizeXxY; i++){
				   for(int j=0; j<sizeXxY; j++){
					   for(int k=0; k<3; k++){
						  System.out.print(" dfrXv["+i+"]["+j+"]["+k+"]="+dfrXv[i][j][k]);
					   }System.out.println(" ");
				   }
			 }*/
			
			System.out.println("\n ");
			 for(int i=0; i<sizeXxY; i++){
				   for(int j=0; j<sizeXxY; j++){
					   for(int k=0; k<3; k++){
						   b[i][j][k]=h*(f[i][j][k]+h*dfrXv[i][j][k]);
						   //System.out.print(b[i][j][k] +"=b["+i+"]["+j+"]["+k+"] ");
					   }//System.out.println(" ");
				   }
			 }
			 
			 /*System.out.println("/nF ");
			 for(int i=0; i<sizeXxY; i++){
				   for(int j=0; j<sizeXxY; j++){
					   for(int k=0; k<3; k++){
						  System.out.print(" F["+i+"]["+j+"]["+k+"]="+f[i][j][k]);
					   }System.out.println(" ");
				   }
			 }*/
			 
		dv=ConjugateGradientMethod(A,b);
		
		return dv;
			 
	}
	
	private double[][][] ConjugateGradientMethod(double[][][] A, double[][][] b){
		 int count=0;
		 int go=1;
		 double vec_AxV[][][] = new double[sizeXxY][sizeXxY][3],
		 	   d[][][] = new double[sizeXxY][sizeXxY][3],
		 	   k1[][][] = new double[sizeXxY][sizeXxY][3],
			   k2[][][] = new double[sizeXxY][sizeXxY][3],
			   mas1[][] = new double[sizeXxY][sizeXxY],
			   mas2[][] = new double[sizeXxY][sizeXxY],
			   alpha[][] = new double[sizeXxY][sizeXxY], 
			   betta[][] = new double[sizeXxY][sizeXxY],
			   dv_pre[][][]= new double[sizeXxY][sizeXxY][3]; 
		 
		 for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				   for(int k=0; k<3; k++){
					   dv_pre[i][j][k]=b[i][j][k];
				   }
			   }
		 }
		 
		/*for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				   for(int k=0; k<3; k++){
					  System.out.print(dv_pre[i][j][k] +"=dv_pre["+i+"]["+j+"]["+k+"] ");
				   }
			   }System.out.println(" ");
		 }*/
		 
		 vec_AxV=MatrixMltplVector(A,dv_pre);
		 
		 /*System.out.println("\n ");
		 for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				   for(int k=0; k<3; k++){
					  System.out.print(vec_AxV[i][j][k] +"=vec_AxV["+i+"]["+j+"]["+k+"] ");
				   }System.out.println(" ");
			   }
		 }*/
		 
		 
		 for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				   for(int k=0; k<3; k++){
					   d[i][j][k]=b[i][j][k]-vec_AxV[i][j][k];
					   //k1[i][j][k]=b[i][j][k]-vec_AxV[i][j][k];
					   k1[i][j][k]=d[i][j][k];
				   }
			   }
		 }
				 
		 /*System.out.println(" ");
		 for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				   for(int k=0; k<9; k++){
					  System.out.print(" A["+i+"]["+j+"]["+k+"]="+A[i][j][k]);
				   }System.out.println(" ");
			   }
		 }*/
		 
		 /*System.out.println("k1=d ");
		 for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				   for(int k=0; k<3; k++){
					  System.out.print(" d["+i+"]["+j+"]["+k+"]="+d[i][j][k]);
				   }
			   }System.out.println(" ");
		 }*/
		 
		 /*System.out.println(" k1===d ");
		 for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				   for(int k=0; k<3; k++){
					  System.out.print(" k1["+i+"]["+j+"]["+k+"]="+k1[i][j][k]);
				   }
			   }System.out.println(" ");
		 }*/
		 
		 //System.out.println("\nfind dv: ");	 
		 while(go==1){	//критерий остановки итерационного процесса
		   //System.out.print(count +" ");	 
			 
		   mas1=Vector1MltplVector2T(k1,null);
		   
		   /*System.out.println("k1*k1T ");
			 for(int i=0; i<sizeXxY; i++){
				   for(int j=0; j<sizeXxY; j++){
					   //for(int k=0; k<3; k++){
						  System.out.print(" mas1["+i+"]["+j+"]="+mas1[i][j]);
					   //}
				   }System.out.println(" ");
			 }
		   
		   	*/   
		   
		   vec_AxV=MatrixMltplVector(A,d);
		   
		   /*System.out.println(" ");
			 for(int i=0; i<sizeXxY*sizeXxY; i++){
				   for(int j=0; j<sizeXxY*sizeXxY; j++){
					   for(int k=0; k<9; k++){
						  System.out.print(" A["+i+"]["+j+"]["+k+"]="+A[i][j][k]);
					   }System.out.println(" ");
				   }
			 }*/
		   
		   
		   /*System.out.println(" ");
			 for(int i=0; i<sizeXxY; i++){
				   for(int j=0; j<sizeXxY; j++){
					   for(int k=0; k<3; k++){
						  System.out.print(" vec_AxV["+i+"]["+j+"]["+k+"]="+vec_AxV[i][j][k]);
					   }System.out.println(" ");
				   }
			 }*/
		   mas2=Vector1MltplVector2T(vec_AxV,d);
		   
		   alpha=division(mas1,mas2);
		   
		   /*for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				   //System.out.print(mas1[i][j] +"=mas1["+i+"]["+j+"]");
				   //System.out.print(mas2[i][j] +"=mas2["+i+"]["+j+"]");
				   System.out.print(alpha[i][j] +"=alpha["+i+"]["+j+"]");
			   }System.out.println(" ");
		 }*/
		   
		   
		   
		   for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				   for(int k=0; k<3; k++){
					   dv_pre[i][j][k]+=alpha[i][j]*d[i][j][k];
				   }
			   }
		   }
		   
		   /*System.out.println(" ");
		   for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				   for(int k=0; k<3; k++){
					  System.out.print(dv_pre[i][j][k] +"=dv_pre["+i+"]["+j+"]["+k+"] ");
				   }System.out.println(" ");
			   }
		   }*/
		   
		   vec_AxV=MatrixMltplVector(A,d);
			 
		   for(int i=0; i<sizeXxY; i++){
				   for(int j=0; j<sizeXxY; j++){
					   for(int k=0; k<3; k++){
						   k2[i][j][k]=k1[i][j][k]-alpha[i][j]*vec_AxV[i][j][k];
					   }
				   }
			 }
		   
		   /*System.out.println(" ");
			 for(int i=0; i<sizeXxY; i++){
				   for(int j=0; j<sizeXxY; j++){
					   for(int k=0; k<3; k++){
						  System.out.print(" k2["+i+"]["+j+"]["+k+"]="+k2[i][j][k]);
					   }System.out.println(" ");
				   }
			 }*/
		   
		  
		   mas2=Vector1MltplVector2T(k2,null);
		   betta=division(mas2,mas1);
		   
		   /*for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				   //System.out.print(mas2[i][j] +"=mas2["+i+"]["+j+"]");
				   //System.out.print(mas1[i][j] +"=mas1["+i+"]["+j+"]");
				   System.out.print(betta[i][j] +"=betta["+i+"]["+j+"]");
			   }System.out.println(" ");
		  }*/
		   
		   for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				   for(int k=0; k<3; k++){
					   d[i][j][k]=k2[i][j][k]+betta[i][j]*d[i][j][k];
				   }
			   }
		  }
		   
		   
		   /*System.out.println(" ");
			 for(int i=0; i<sizeXxY; i++){
				   for(int j=0; j<sizeXxY; j++){
					   for(int k=0; k<3; k++){
						  System.out.print(" d["+i+"]["+j+"]["+k+"]="+d[i][j][k]);
					   }System.out.println(" ");
				   }
			 }*/
		   
		   
		  for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				   for(int k=0; k<3; k++){
					   k1[i][j][k]=k2[i][j][k];
				   }
			   }
		  }
		  
		  /*System.out.println(" новое k1");	 
		  for(int i=0; i<sizeXxY; i++){
				   for(int j=0; j<sizeXxY; j++){
					   for(int k=0; k<3; k++){
						  System.out.print(" k1["+i+"]["+j+"]["+k+"]="+k1[i][j][k]);
					   }
				   }System.out.println(" ");
			 }*/
		  
		  /*try {
			System.in.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		  
		   int rep=0; 
			  for(int i=0; i<sizeXxY; i++){
				   for(int j=0; j<sizeXxY; j++){
					   for(int k=0; k<3; k++){
						   if(k2[i][j][k]>eps) {rep=1;/*System.out.println(" ***** ");*/ break;}
						   if(k2[sizeXxY-1][sizeXxY-1][k]<eps) {go=0;/*System.out.println(" *YES* ");*/} 
						   if(k2[i][j][k]==Double.NaN) {/*System.out.println(" *YES* ");*/ start =0; rep=1; break;}
					   }
					   if(rep==1) break;
				   }
				   if(rep==1) {count++; 
					  if(count<num_iteration) {go=1;}
					  else go=0;
					  break;}
				   
			  }
			   
			  
			// конец while  
		  }
		  
		 
		 /*System.out.println(" ");
		 for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				   for(int k=0; k<3; k++){
					  System.out.print(dv_pre[i][j][k] +"=dv_pre["+i+"]["+j+"]["+k+"] ");
				   }
			   }System.out.println(" ");
		 }*/
		 
		 return dv_pre;
	}
	
	private double[][] division(double[][] mas1, double[][] mas2) {
		double res[][] = new double[sizeXxY][sizeXxY]; 
		
		for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				   res[i][j]=mas1[i][j]/mas2[i][j];	
			   }
		}
		
		return res;
	}


	private double[][] Vector1MltplVector2T(double[][][] v1, double[][][] v2){
		double value[][] = new double[sizeXxY][sizeXxY];
		
		if(v2==null) v2=v1;
		for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				  value[i][j]=v1[i][j][0]*v2[i][j][0]+
						  	  v1[i][j][1]*v2[i][j][1]+
						  	  v1[i][j][2]*v2[i][j][2];
			   }
	    }
		
	return value;
			
	}
	
	
	private void CSR(double[][][] A){
		CSR_elem = new double[sizeXxY*sizeXxY*sizeXxY*sizeXxY][9];
		CSR_iptr = new int[sizeXxY*sizeXxY+1];
		CSR_jptr = new int[sizeXxY*sizeXxY*sizeXxY*sizeXxY];
		CSR_size=0;
		
		int cnt=0,
			nextRow=0,
			row=0,
			i_ch=0;
		
		for(int i=0; i<sizeXxY*sizeXxY; i++){
			   for(int j=0; j<sizeXxY*sizeXxY; j++){
				   if(    A[i][j][0]==0 && A[i][j][1]==0 && A[i][j][2]==0
					   && A[i][j][3]==0 && A[i][j][4]==0 && A[i][j][5]==0
					   && A[i][j][6]==0 && A[i][j][7]==0 && A[i][j][8]==0){/*System.out.println("!!!");*/continue;}
				   else{ 
					   
					   if(i_ch!=i)nextRow=0;
					   i_ch=i;
					   CSR_elem[cnt][0]=A[i][j][0];
					   CSR_elem[cnt][1]=A[i][j][1];
					   CSR_elem[cnt][2]=A[i][j][2];
					   CSR_elem[cnt][3]=A[i][j][3];
					   CSR_elem[cnt][4]=A[i][j][4];
					   CSR_elem[cnt][5]=A[i][j][5];
					   CSR_elem[cnt][6]=A[i][j][6];
					   CSR_elem[cnt][7]=A[i][j][7];
					   CSR_elem[cnt][8]=A[i][j][8];
			           if(nextRow==0){CSR_iptr[row]=cnt; nextRow=1; row++;}
	                   CSR_jptr[cnt]=j;
	                   cnt++;
				   }
			   
			   }
			   if(i==sizeXxY*sizeXxY-1){CSR_iptr[row]=cnt;}
	   }
	   CSR_size=cnt;
		
	}
	
	
	private double[][][] MatrixMltplVector(double[][][] Matrix, double[][][] Vector ) {
		double vec_new[][] = new double[sizeXxY*sizeXxY][3],
			  vec_N[][] = new double[sizeXxY*sizeXxY][3],
			  vec_XxY[][][] = new double[sizeXxY][sizeXxY][3]; 	// ...
		int ax = 0,ay = 0,az = 0;
		
		
		CSR(Matrix);
		vec_N=resizeXxYtoN(Vector);
		
				
		for(int i=0; i<sizeXxY*sizeXxY; i++){
			for(int k=0; k<3; k++){
				vec_new[i][k]=0;
				if(k==0){ax=0; ay=1; az=2;}
				if(k==1){ax=3; ay=4; az=5;}
				if(k==2){ax=6; ay=7; az=8;}
				
				for(int j=CSR_iptr[i]; j<CSR_iptr[i+1]; j++){
					vec_new[i][k]+=CSR_elem[j][ax]*vec_N[i][0]
							 + CSR_elem[j][ay]*vec_N[i][1]
							 + CSR_elem[j][az]*vec_N[i][2];
				}
				
			}
		}
		
		vec_XxY=resizeNtoXxY(vec_new);
		
	return vec_XxY;
	}
	
	private double[][][] resizeNtoXxY(double[][] vec) {
		double v_new[][][] = new double[sizeXxY][sizeXxY][3];
		int s=0;
		
		for(int i=0; i<sizeXxY; i++){
			  for(int j=0; j<sizeXxY; j++){
				  v_new[i][j][0]=vec[s][0];
				  v_new[i][j][1]=vec[s][1];
				  v_new[i][j][2]=vec[s][2];
				  s++; 
			 }
		}
		
		return v_new;
	}

	private double[][] resizeXxYtoN(double[][][] vector) {
		double v_new[][] = new double[sizeXxY*sizeXxY][3];
		int s=0;
		
		for(int i=0; i<sizeXxY; i++){
			  for(int j=0; j<sizeXxY; j++){
				  v_new[s][0]=vector[i][j][0];
				  v_new[s][1]=vector[i][j][1];
				  v_new[s][2]=vector[i][j][2];
				  s++; 
			 }
		}
		
		return v_new;
	}


	private double getMass(int a, int b, int k) {
		if(a==b && (k==0 || k==4 || k==8))return mass1;
		else return 0;
	}
	
	
	
	private  double dF_v(int a, int b, int k) {
		if(a==b && (k==0 || k==4 || k==8))return (-1)*c;
		else return 0;
	}
	
	
	private double[][][] dF_r() {
		int N=(int) Math.pow(sizeXxY, 2);
		double dfr[][][] = new double[N][N][9];
		double df_r_3x3[][] = new double[3][3];
		int XY[]=new int[2],
			i1,j1,	//положение первой точки
			i2,j2;	//положение второй точки
	
		for(int i=0; i<N; i++){
			   XY=getNtoXY(i);
			   i1=XY[0];
			   j1=XY[1];
			   
			   for(int j=0; j<N; j++){
				  
				 for(int z=0; z<9; z++){
					  dfr[i][j][z]=0;
				 }
				  	  			   
					   XY=getNtoXY(j);
					   i2=XY[0];
					   j2=XY[1];
					   
				   // strain_compression
				   if((i1==i2 && Math.abs(j1-j2)==1) || (j1==j2 && Math.abs(i1-i2)==1)) {
					   koef=1;
					   df_r_3x3=dF_r_xyz(i1,j1,i2,j2);
					   dfr[i][j][0]= df_r_3x3[0][0];
					   dfr[i][j][1]= df_r_3x3[0][1];
					   dfr[i][j][2]= df_r_3x3[0][2];
					   dfr[i][j][3]= df_r_3x3[1][0];		///
					   dfr[i][j][4]= df_r_3x3[1][1];
					   dfr[i][j][5]= df_r_3x3[1][2];
					   dfr[i][j][6]= df_r_3x3[2][0];		///
					   dfr[i][j][7]= df_r_3x3[2][1];		///
					   dfr[i][j][8]= df_r_3x3[2][2];
				   } 
				   
				   
				   // translation
				   if( Math.abs(j1-j2)==1 && Math.abs(i1-i2)==1 ) {
					   koef=2;
					   df_r_3x3=dF_r_xyz(i1,j1,i2,j2);
					   dfr[i][j][0]=df_r_3x3[0][0];
					   dfr[i][j][1]=df_r_3x3[0][1];
					   dfr[i][j][2]=df_r_3x3[0][2];
					   dfr[i][j][3]=df_r_3x3[1][0];		///
					   dfr[i][j][4]=df_r_3x3[1][1];
					   dfr[i][j][5]=df_r_3x3[1][2];
					   dfr[i][j][6]=df_r_3x3[2][0];		///
					   dfr[i][j][7]=df_r_3x3[2][1];		///
					   dfr[i][j][8]=df_r_3x3[2][2];
				   } 
				   
				   
				   // bending
				   if((i1==i2 && Math.abs(j1-j2)==2) || (j1==j2 && Math.abs(i1-i2)==2)) {
					   koef=3;
					   df_r_3x3=dF_r_xyz(i1,j1,i2,j2);
					   dfr[i][j][0]=df_r_3x3[0][0];
					   dfr[i][j][1]=df_r_3x3[0][1];
					   dfr[i][j][2]=df_r_3x3[0][2];
					   dfr[i][j][3]=df_r_3x3[1][0];		///
					   dfr[i][j][4]=df_r_3x3[1][1];
					   dfr[i][j][5]=df_r_3x3[1][2];
					   dfr[i][j][6]=df_r_3x3[2][0];		///
					   dfr[i][j][7]=df_r_3x3[2][1];		///
					   dfr[i][j][8]=df_r_3x3[2][2];
				   }   
	
			   }
		}
		
		for(int i=0; i<N; i++){
			   for(int j=0; j<N; j++){
				  for(int z=0; z<9; z++){
					  if(i>0 && j<i && dfr[j][i][z]!=0) {
						   dfr[i][j][z]=dfr[j][i][z];
						   //System.out.println(" ***dfr[i][j]["+z+"]="+ dfr[i][j][z]);   
					  }
					  
				  }
			  }
	   }
		
		for(int i=0; i<N; i++){
			 for(int z=0; z<9; z++){
				 for(int j=0; j<N; j++){
					 if(j!=i)dfr[i][i][z]+=(-1.0d)*dfr[i][j][z];
				}
			}
			  
		}
		
		/*System.out.println("");
		for(int i=0; i<N; i++){
			 for(int j=0; j<N; j++){
				 for(int z=0; z<9; z++){
					 //if(z==0 || z==4 || z==8 )
					 //if(j==1 && i==0 )
						System.out.print(" dfr["+i+"]["+j+"]["+z+"]="+dfr[i][j][z]);  
				}System.out.println("");
			 }
		 }*/
				
		return dfr;
	}


	private int[] getNtoXY(int x) {
		int XY[]=new int[2],
			s=-1;
		
		for(int i=0; i<sizeXxY; i++){
			  for(int j=0; j<sizeXxY; j++){
			  s++;
			  if(s==x) {XY[0]=i; XY[1]=j; break;}
			  }
			  if(s==x)break;
		}
		return XY;
	}
	
	
	// массив 3x3, элементов не нулулевых ячеек
	private double[][] dF_r_xyz(int i, int j, int ii, int jj) {
		double dfr_xyz[][] = new double[3][3];
	   
	    for(int a=0; a<3; a++){
			   for(int b=0; b<3; b++){
				  
				   if(a==b) {
						   dfr_xyz[a][b]=get_dF_diag(a,i,j,ii,jj);
						   continue;
						   }
				   
				   if(a>0 && b<a) {
						   dfr_xyz[a][b]=dfr_xyz[b][a];
						   continue;
						  }
				  
				  dfr_xyz[a][b]=get_dF(a,b, i,j, ii,jj);
			   }
	    }
	    
	    return dfr_xyz;
	}
	
	
	private double get_dF(int a, int b, int i, int j, int ii, int jj) {
		double len0=0;
		double k=0;
		if(koef==1) {k=ks*100; len0=XstepX;/*System.out.println("///ks  ");*/}
		if(koef==2) {k=kb*100; len0=(double) Math.sqrt(2*XstepX*XstepX);/*System.out.println("///kb  ");*/}
		if(koef==3) {k=kt*100; len0=2*XstepX;/*System.out.println("///kt  ");*/}
		
		return (-1)*k*(((len0 *QuadAxLen(i,j,ii,jj,b,a))
				))/length(i,j,ii,jj) ;
		
	}
	
	private double get_dF_diag(int a, int i, int j, int ii, int jj) {
		int ax1 = 0,ax2 = 0;
		double k=0;
		double len0=0;
		
		if(a==0) {ax1=1; ax2=2;}
		if(a==1) {ax1=0; ax2=2;}
		if(a==2) {ax1=0; ax2=1;}
		
		if(koef==1) {k=ks*100; len0=XstepX; /*System.out.println("///ks  ");*/}
		if(koef==2) {k=kb*100; len0=Math.sqrt(2*XstepX*XstepX);/*System.out.println("///kb  ");*/}
		if(koef==3) {k=kt*100; len0=2*XstepX;/*System.out.println("///kt  ");*/}
		
		double y=len0*(QuadAxLen(i,j,ii,jj,ax1,-1)+QuadAxLen(i,j,ii,jj,ax2,-1));
		double t=(-1)*length(i,j,ii,jj)+y;
		double z=t/length(i,j,ii,jj);
		
		return k*z;
		
	}

	private double length(int i, int j, int ii, int jj) {
		return Math.abs(Math.pow(Math.sqrt(Math.pow(P[ii][jj][0] - P[i][j][0], 2) + Math.pow(P[ii][jj][1] - P[i][j][1], 2) + Math.pow(P[ii][jj][2] - P[i][j][2], 2)), 3));
	}

	private double QuadAxLen(int i1, int j1,int i2, int j2, int ax1, int ax2 ) {
		if(ax2==-1)ax2=ax1;
		return ((P[i1][j1][ax1]-P[i2][j2][ax1])*(P[i1][j1][ax2]-P[i2][j2][ax2]));
	}
	
	   /*================================================================*/
	   /*Падение ткани. Метод Рунге-Кутты 4-ого порядка*/
	   /*================================================================*/   
	   private void doShitFall3(GL gl) {
		   		   
		   vSumm = findNewPnext(); 
		 		   
		   //скорости
		   for(int i=0; i<sizeXxY; i++) {
			   for(int j=0; j<sizeXxY; j++){
				   for(int k=0; k<3; k++){
		               vRK[i][j][k] += h*(vSumm[i][j][k]/6); 
		               //System.out.print(" vRK="+vRK[i][j][k]);
		           }//System.out.println("");
			   }
		   }
		   
		   		   
		   //новое положение
		   for(int i=0; i<sizeXxY; i++){
			   for(int j=0; j<sizeXxY; j++){
				   if(static_points==1){if(i==0 && j==0) continue;}
				   if(static_points==2){if(i==0 && j==0) continue;
				   						if(i==0 && j==sizeXxY-1) continue;}
				   if(static_points==3){if(i==0 && j==0) continue;
										if(i==0 && j==sizeXxY-1) continue;
										if(i==0 && j==sizeXxY/2) continue;}
				   if(static_points==4){if(i==0 && j==0) continue;
										if(i==0 && j==sizeXxY-1) continue;
										if(i==0 && j==sizeXxY/3) continue;
										if(i==0 && j==2*sizeXxY/3) continue;}
				   if(static_points==5){if(i==0 && j==0) continue;
										if(i==0 && j==sizeXxY-1) continue;
										if(i==0 && j==sizeXxY/4) continue;
										if(i==0 && j==2*sizeXxY/4) continue;
										if(i==0 && j==3*sizeXxY/4) continue;}
				   for(int k=0; k<3; k++){
		               P[i][j][k] += h* vRK[i][j][k];
		           }
		       }
		   }
		   
		  
	 
	   }
	   
   private double[][][] findNewPnext(){
	   double[][][]    vel = new double[sizeXxY][sizeXxY][3],
					   k1 = new double[sizeXxY][sizeXxY][3],
					   k2 = new double[sizeXxY][sizeXxY][3],
					   k3 = new double[sizeXxY][sizeXxY][3],
					   k4 = new double[sizeXxY][sizeXxY][3],
					   kSumm = new double[sizeXxY][sizeXxY][3];
	   
	  //внешние силы
	   for(int i=0; i<sizeXxY; i++){ 				
		   for(int j=0; j<sizeXxY; j++){				
	           f[i][j][0] =(-1)* c * vRK[i][j][0];
	           f[i][j][1] =(-1)* c * vRK[i][j][1];	
	           f[i][j][2] =(-1)* c * vRK[i][j][2] - grav_free_fall*mass1 ;
	       }
	   }
	   		   
	   double len;
	   //внутренние силы
	   for(int i=0; i<sizeXxY; i++){ 				
		   for(int j=0; j<sizeXxY; j++){	
			   for(int k=-1; k<=1; k++){
		            if (i+k<0 || i+k>=sizeXxY) continue;
		            for(int l=-1; l<=1; l++){
		                if (j+l<0 || j+l>=sizeXxY) continue;
		                len=getDistance(P[i][j][0],P[i][j][1],P[i][j][2],P[i+k][j+l][0],P[i+k][j+l][1],P[i+k][j+l][2]);                                                                          
		                for(int m=0; m<3; m++){
		                    if (Math.abs(k)+Math.abs(l)==1){f[i][j][m] += addInnerForce(P[i][j][m],P[i+k][j+l][m],ks*100,XstepX,len);}
		                    if (Math.abs(k)+Math.abs(l)==2){f[i][j][m] += addInnerForce(P[i][j][m],P[i+k][j+l][m],kb*100,Math.sqrt(2*XstepX*XstepX),len);}
		                }
		            }
		        }
			   for(int k=-2; k<=2; k+=2){
		            if (i+k<0 || i+k>=sizeXxY) continue;
		            for(int l=-2; l<=2; l+=2){
		                if (j+l<0 || j+l>=sizeXxY) continue;
		                len=getDistance(P[i][j][0],P[i][j][1],P[i][j][2],P[i+k][j+l][0],P[i+k][j+l][1],P[i+k][j+l][2]);    
		                for(int m=0; m<3; m++){
		                	if (Math.abs(k)+Math.abs(l)==2){f[i][j][m] += addInnerForce(P[i][j][m],P[i+k][j+l][m],kt*100,2*XstepX,len);}
		                }
		            }
	           }
		   }
	   }
	   
	   /*for(int i=0; i<sizeXxY; i++) {
		   for(int j=0; j<sizeXxY; j++){
			   for(int k=0; k<3; k++){
				   v[i][j][k] = vRK[i][j][k];
				   vel[i][j][k]=v[i][j][k];
				   k1[i][j][k]=v[i][j][k];
				   //kSumm[i][j][k]=v[i][j][k];
				   kSumm[i][j][k]=k1[i][j][k];
			   }
		   }
	   }*/
	   
	   //findForce();
	   for(int i=0; i<sizeXxY; i++) {
		   for(int j=0; j<sizeXxY; j++){
			   for(int k=0; k<3; k++){
				   vel[i][j][k]=h*(f[i][j][k]/mass1);
				   v[i][j][k]=h*(f[i][j][k]/mass1);
				   //vel[i][j][k]=v[i][j][k];
				   //vel[i][j][k]=vRK[i][j][k];//+h*(f[i][j][k]/mass1);
				   //v[i][j][k]=vel[i][j][k];
				   //v[i][j][k]=vRK[i][j][k];//+h*(f[i][j][k]/mass1);
				   k1[i][j][k]=vel[i][j][k];
				   kSumm[i][j][k]=vel[i][j][k];
			   }
		   }
	   }
	   
	   /*for(int i=0; i<sizeXxY; i++) {
		   for(int j=0; j<sizeXxY; j++){
			   for(int k=0; k<3; k++){
				   kSumm[i][j][k]=v[i][j][k];
			   }
		   }
	   }*/
	   	   
					   /*System.out.println("------------k1");  
					   for(int i=0; i<sizeXxY; i++) {
						   for(int j=0; j<sizeXxY; j++){
							   for(int k=0; k<3; k++){
								   System.out.print(" k1="+k1[i][j][k]);
							   }System.out.println("");
						   }
					   }
					   
					   for(int i=0; i<sizeXxY; i++) {
						   for(int j=0; j<sizeXxY; j++){
							   for(int k=0; k<3; k++){
								   System.out.print(" v="+v[i][j][k]);
							   }System.out.println("");
						   }
					   }
					   
					   System.out.println("------------kSumm");  
					   for(int i=0; i<sizeXxY; i++) {
						   for(int j=0; j<sizeXxY; j++){
							   for(int k=0; k<3; k++){
								   System.out.print(" kSumm="+kSumm[i][j][k]);
							   }System.out.println("");
						   }
					   }*/
	   
	   //System.out.println("----------Ptmp");
	   //новое положение
	   for(int i=0; i<sizeXxY; i++){
		   for(int j=0; j<sizeXxY; j++){
			   if(static_points==1){if(i==0 && j==0) continue;}
			   if(static_points==2){if(i==0 && j==0) continue;
			   						if(i==0 && j==sizeXxY-1) continue;}
			   if(static_points==3){if(i==0 && j==0) continue;
									if(i==0 && j==sizeXxY-1) continue;
									if(i==0 && j==sizeXxY/2) continue;}
			   if(static_points==4){if(i==0 && j==0) continue;
									if(i==0 && j==sizeXxY-1) continue;
									if(i==0 && j==sizeXxY/3) continue;
									if(i==0 && j==2*sizeXxY/3) continue;}
			   if(static_points==5){if(i==0 && j==0) continue;
									if(i==0 && j==sizeXxY-1) continue;
									if(i==0 && j==sizeXxY/4) continue;
									if(i==0 && j==2*sizeXxY/4) continue;
									if(i==0 && j==3*sizeXxY/4) continue;}
			   for(int k=0; k<3; k++){
	               Ptmp[i][j][k] += h* k1[i][j][k];
	               //System.out.print(" Ptmp="+Ptmp[i][j][k]);
			   }//System.out.println("");
	       }
	   }
	   
	   findForce();		   
	   newVelocity(vel);
	   for(int i=0; i<sizeXxY; i++) {
		   for(int j=0; j<sizeXxY; j++){
			   for(int k=0; k<3; k++){
				   //k2[i][j][k]=v[i][j][k];
				   k2[i][j][k]=vel[i][j][k]+(h/2)*k1[i][j][k];
			   }
		   }
	   }
	   for(int i=0; i<sizeXxY; i++) {
		   for(int j=0; j<sizeXxY; j++){
			   for(int k=0; k<3; k++){
				   //kSumm[i][j][k]+=2*v[i][j][k];
				   kSumm[i][j][k]+=2*k2[i][j][k];
			   }
		   }
	   }
	  	   
					   /*System.out.println("------------k2");  
					   for(int i=0; i<sizeXxY; i++) {
						   for(int j=0; j<sizeXxY; j++){
							   for(int k=0; k<3; k++){
								   System.out.print(" k1="+k2[i][j][k]);
							   }System.out.println("");
						   }
					   }
					   
					   for(int i=0; i<sizeXxY; i++) {
						   for(int j=0; j<sizeXxY; j++){
							   for(int k=0; k<3; k++){
								   System.out.print(" v="+v[i][j][k]);
							   }System.out.println("");
						   }
					   }
					   
					   System.out.println("------------kSumm");  
					   for(int i=0; i<sizeXxY; i++) {
						   for(int j=0; j<sizeXxY; j++){
							   for(int k=0; k<3; k++){
								   System.out.print(" kSumm="+kSumm[i][j][k]);
							   }System.out.println("");
						   }
					   }*/

	   
	   
	   //findForce();		   
	   //newVelocity(vel);
	   for(int i=0; i<sizeXxY; i++) {
		   for(int j=0; j<sizeXxY; j++){
			   for(int k=0; k<3; k++){
				   k3[i][j][k]=vel[i][j][k]+(h/2)*k2[i][j][k];
			   }
		   }
	   }
	   for(int i=0; i<sizeXxY; i++) {
		   for(int j=0; j<sizeXxY; j++){
			   for(int k=0; k<3; k++){
				   //kSumm[i][j][k]+=2*v[i][j][k];
				   kSumm[i][j][k]+=2*k3[i][j][k];
			   }
		   }
	   }
	   
					   /*System.out.println("------------k3");  
					   for(int i=0; i<sizeXxY; i++) {
						   for(int j=0; j<sizeXxY; j++){
							   for(int k=0; k<3; k++){
								   System.out.print(" k1="+k3[i][j][k]);
							   }System.out.println("");
						   }
					   }
					   
					   for(int i=0; i<sizeXxY; i++) {
						   for(int j=0; j<sizeXxY; j++){
							   for(int k=0; k<3; k++){
								   System.out.print(" v="+v[i][j][k]);
							   }System.out.println("");
						   }
					   }
					   
					   System.out.println("------------kSumm");  
					   for(int i=0; i<sizeXxY; i++) {
						   for(int j=0; j<sizeXxY; j++){
							   for(int k=0; k<3; k++){
								   System.out.print(" kSumm="+kSumm[i][j][k]);
							   }System.out.println("");
						   }
					   }*/

	   
	   
	   //findForce();		   
	   //newVelocity(vel);
	   for(int i=0; i<sizeXxY; i++) {
		   for(int j=0; j<sizeXxY; j++){
			   for(int k=0; k<3; k++){
				   k4[i][j][k]=vel[i][j][k]+(h/2)*k3[i][j][k];
			   }
		   }
	   }
	   for(int i=0; i<sizeXxY; i++) {
		   for(int j=0; j<sizeXxY; j++){
			   for(int k=0; k<3; k++){
				   //kSumm[i][j][k]+=v[i][j][k];
				   kSumm[i][j][k]+=k4[i][j][k];
			   }
		   }
	   }
	   
   					   /*System.out.println("------------k4");  
					   for(int i=0; i<sizeXxY; i++) {
						   for(int j=0; j<sizeXxY; j++){
							   for(int k=0; k<3; k++){
								   System.out.print(" k4="+k4[i][j][k]);
							   }System.out.println("");
						   }
					   }
					   
					   for(int i=0; i<sizeXxY; i++) {
						   for(int j=0; j<sizeXxY; j++){
							   for(int k=0; k<3; k++){
								   System.out.print(" v="+v[i][j][k]);
							   }System.out.println("");
						   }
					   }
					   
					   System.out.println("------------kSumm");  
					   for(int i=0; i<sizeXxY; i++) {
						   for(int j=0; j<sizeXxY; j++){
							   for(int k=0; k<3; k++){
								   System.out.print(" kSumm="+kSumm[i][j][k]);
							   }System.out.println("");
						   }
					   }*/
	   
	  
	   return kSumm;
	   
   }
   
   private void newVelocity(double[][][] vel){
	   
	   for(int i=0; i<sizeXxY; i++) {
		   for(int j=0; j<sizeXxY; j++){
			   for(int k=0; k<3; k++){
				    v[i][j][k] = vel[i][j][k] + (h/2)*(f[i][j][k]/mass1); 
				    //v[i][j][k] += (h/2)*(f[i][j][k]/mass1); 
			   }
		   }
	   }
	 
   }
   
   private void findForce(){
   	   
		 //внешние силы
		   for(int i=0; i<sizeXxY; i++){ 				
			   for(int j=0; j<sizeXxY; j++){				
		           f[i][j][0] =(-1)* c * v[i][j][0];
		           f[i][j][1] =(-1)* c * v[i][j][1];	
		           f[i][j][2] =(-1)* c * v[i][j][2] - grav_free_fall*mass1 ;
		       }
		   }
		   		   
		   double len;
		   //внутренние силы
		   for(int i=0; i<sizeXxY; i++){ 				
			   for(int j=0; j<sizeXxY; j++){	
				   for(int k=-1; k<=1; k++){
			            if (i+k<0 || i+k>=sizeXxY) continue;
			            for(int l=-1; l<=1; l++){
			                if (j+l<0 || j+l>=sizeXxY) continue;
			                len=getDistance(Ptmp[i][j][0],Ptmp[i][j][1],Ptmp[i][j][2],Ptmp[i+k][j+l][0],Ptmp[i+k][j+l][1],Ptmp[i+k][j+l][2]);                                                                          
			                for(int m=0; m<3; m++){
			                    if (Math.abs(k)+Math.abs(l)==1){f[i][j][m] += addInnerForce(Ptmp[i][j][m],Ptmp[i+k][j+l][m],ks*100,XstepX,len);}
			                    if (Math.abs(k)+Math.abs(l)==2){f[i][j][m] += addInnerForce(Ptmp[i][j][m],Ptmp[i+k][j+l][m],kb*100,Math.sqrt(2*XstepX*XstepX),len);}
			                }
			            }
			        }
				   for(int k=-2; k<=2; k+=2){
			            if (i+k<0 || i+k>=sizeXxY) continue;
			            for(int l=-2; l<=2; l+=2){
			                if (j+l<0 || j+l>=sizeXxY) continue;
			                len=getDistance(Ptmp[i][j][0],Ptmp[i][j][1],Ptmp[i][j][2],Ptmp[i+k][j+l][0],Ptmp[i+k][j+l][1],Ptmp[i+k][j+l][2]);    
			                for(int m=0; m<3; m++){
			                	if (Math.abs(k)+Math.abs(l)==2){f[i][j][m] += addInnerForce(Ptmp[i][j][m],Ptmp[i+k][j+l][m],kt*100,2*XstepX,len);}
			                }
			            }
		           }
			   }
		   }
				   
	   }
   
   /*================================================================*/
   /*Падение ткани. Явный метод Эйлера*/
   /*================================================================*/   
   private void doShitFall(GL gl) {
	   
	   //новое положение
	   for(int i=0; i<sizeXxY; i++){
		   for(int j=0; j<sizeXxY; j++){
			   if(static_points==1){if(i==0 && j==0) continue;}
			   if(static_points==2){if(i==0 && j==0) continue;
			   						if(i==0 && j==sizeXxY-1) continue;}
			   if(static_points==3){if(i==0 && j==0) continue;
									if(i==0 && j==sizeXxY-1) continue;
									if(i==0 && j==sizeXxY/2) continue;}
			   if(static_points==4){if(i==0 && j==0) continue;
									if(i==0 && j==sizeXxY-1) continue;
									if(i==0 && j==sizeXxY/3) continue;
									if(i==0 && j==2*sizeXxY/3) continue;
									}
			   if(static_points==5){if(i==0 && j==0) continue;
									if(i==0 && j==sizeXxY-1) continue;
									if(i==0 && j==sizeXxY/4) continue;
									if(i==0 && j==2*sizeXxY/4) continue;
									if(i==0 && j==3*sizeXxY/4) continue;}
			   for(int k=0; k<3; k++){
	               P[i][j][k] += h * v[i][j][k];
	           }
	       }
	   }
	   
	   
	   //внешние силы
	   for(int i=0; i<sizeXxY; i++){ 				
		   for(int j=0; j<sizeXxY; j++){				
	           f[i][j][0] =(-1)* c * v[i][j][0];
	           f[i][j][1] =(-1)* c * v[i][j][1];	
	           f[i][j][2] =(-1)* c * v[i][j][2] - grav_free_fall*mass1 ;
	       }
	   }
	   
	   
	   double len;
	   //внутренние силы
	   for(int i=0; i<sizeXxY; i++){ 				
		   for(int j=0; j<sizeXxY; j++){	
			   for(int k=-1; k<=1; k++){
		            if (i+k<0 || i+k>=sizeXxY) continue;
		            for(int l=-1; l<=1; l++){
		                if (j+l<0 || j+l>=sizeXxY) continue;
		                len=getDistance(P[i][j][0],P[i][j][1],P[i][j][2],P[i+k][j+l][0],P[i+k][j+l][1],P[i+k][j+l][2]);                                                                          
		                for(int m=0; m<3; m++){
		                    if (Math.abs(k)+Math.abs(l)==1){f[i][j][m] += addInnerForce(P[i][j][m],P[i+k][j+l][m],ks*100,XstepX,len);}
		                    if (Math.abs(k)+Math.abs(l)==2){f[i][j][m] += addInnerForce(P[i][j][m],P[i+k][j+l][m],kb*100,Math.sqrt(2*XstepX*XstepX),len);}
		                }
		            }
		        }
			   for(int k=-2; k<=2; k+=2){
		            if (i+k<0 || i+k>=sizeXxY) continue;
		            for(int l=-2; l<=2; l+=2){
		                if (j+l<0 || j+l>=sizeXxY) continue;
		                len=getDistance(P[i][j][0],P[i][j][1],P[i][j][2],P[i+k][j+l][0],P[i+k][j+l][1],P[i+k][j+l][2]);    
		                for(int m=0; m<3; m++){
		                	if (Math.abs(k)+Math.abs(l)==2){f[i][j][m] += addInnerForce(P[i][j][m],P[i+k][j+l][m],kt*100,2*XstepX,len);}
		                }
		            }
	           }
		   }
	   }
	   
	   //скорости
	   for(int i=0; i<sizeXxY; i++) {
		   for(int j=0; j<sizeXxY; j++){
			   for(int k=0; k<3; k++){
	               v[i][j][k] += h* f[i][j][k]/mass1; 
	               //System.out.print(" v="+v[i][j][k]);
	           }//System.out.println("");
		   }
	   }
 
   }

   private double getDistance(double x1, double y1, double z1, double x2, double y2, double z2)
   {
	   return  Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) + Math.pow(z2 - z1, 2));
   }

   private double addInnerForce(double pos1, double pos2, double k, double distance0, double distance1)
   {
       return (k * ((pos2 - pos1)*(1-distance0/distance1)) );
   }

    
   /*================================================================*/
   /*Прорисовка ткани*/
   /*================================================================*/
   private void doShitShow(GL gl) {
	   // отображение
	   gl.glColor3d(0.5, 0.7, 0.5);
	   gl.glEnable(GL.GL_LINE_SMOOTH);
	   gl.glLineWidth(0.5f);
	   gl.glBegin(GL.GL_LINES);
	   gl.glColor3d(0,0,0.5); 		// синий
	   gl.glColor3d(0,0.5,0); 		// зеленый
	   for(int i=0; i<sizeXxY-1; i++) {
    	   	for(int j=0; j<sizeXxY; j++) {
    	   		gl.glVertex3d(P[i][j][0],P[i][j][2],P[i][j][1]);
    	   		gl.glVertex3d(P[i+1][j][0],P[i+1][j][2],P[i+1][j][1]);
    	   	}
	   }
	   for(int i=0; i<sizeXxY; i++) {
   	   	for(int j=0; j<sizeXxY-1; j++) {
   	   		gl.glVertex3d(P[i][j][0],P[i][j][2],P[i][j][1]);
   	   		gl.glVertex3d(P[i][j+1][0],P[i][j+1][2],P[i][j+1][1]);
   	   	}
	   }
	   
	   gl.glEnd();
   }

   
   /*================================================================*/
   /*Расчет сетки точек*/
   /*================================================================*/
   public void doGridPoints(){
	   
	   for(int i=0; i<sizeXxY; i++) {
		   P[i][0][0]=0.0d;
		   P[i][0][1]=i*stepY;
		   P[i][0][2]=0.0d;
		   Ptmp[i][0][0] = P[i][0][0];
		   Ptmp[i][0][1] = P[i][0][1];
		   Ptmp[i][0][2] = P[i][0][2];
		   for(int j=1; j<sizeXxY; j++) {
			   P[i][j][0]=P[i][j-1][0] + stepX;
			   P[i][j][1]=P[i][j-1][1];
			   P[i][j][2]=0.0d;
			   Ptmp[i][j][0] = P[i][j][0];
			   Ptmp[i][j][1] = P[i][j][1];
			   Ptmp[i][j][2] = P[i][j][2];
			}
	   }
	  
	   
	   for(int i=0; i<sizeXxY; i++) {
		   for(int j=0; j<sizeXxY; j++)  {
			   for(int k=0; k<3; k++){
	               v[i][j][k] = 0.0d;
			   	   vRK[i][j][k] = 0.0d;
			   	   }
	       }
	   }
   }

	/*================================================================*/
	/*Создание осевых линий*/
	/*================================================================*/
   public void doGridZero(GL gl){
	 
    gl.glEnable(GL.GL_LINE_SMOOTH);
    gl.glLineWidth(0.5f);
    gl.glBegin(GL.GL_LINES);
    		
	// 3 осевые линии
	gl.glColor3d(1, 0, 0);
	gl.glVertex3d(0.0d, 0.0d, 0.0d);
	gl.glVertex3d(0.0d, 0.0d, 0.0d-10);
	
	gl.glVertex3d(0.0d, 0.0f, 0.0d);
	gl.glVertex3d(10.0f, 0.0f, 0.0d);
	
	gl.glVertex3d(0.0f, 0.0f, 0.0d);
	gl.glVertex3d(0.0f, 10.0f, 0.0d);
	gl.glEnd();
    
    gl.glEnable(GL.GL_POINT_SMOOTH);
    gl.glPointSize(4);
    gl.glBegin(GL.GL_POINTS);
    gl.glColor3d(0,0.5,1);
    gl.glVertex3d(0,0,0);
    gl.glVertex3d((sizeXxY-1)*stepX,0.0d,(sizeXxY-1)*stepX);
    gl.glEnd();	
}
 	
	/*================================================================*/
	/*  */
	/*================================================================*/
   public void displayChanged(GLAutoDrawable gLDrawable, boolean modeChanged, boolean deviceChanged) {
   }

   /*================================================================*/
   /*   */
   /*================================================================*/
   public void init(GLAutoDrawable gLDrawable) {
       final GL gl = gLDrawable.getGL();
   
       gl.glShadeModel(GL.GL_SMOOTH);
       gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
       gl.glClearDepth(1.0f);
       gl.glEnable(GL.GL_DEPTH_TEST);
       gl.glDepthFunc(GL.GL_LEQUAL);
       gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT,GL.GL_NICEST);
       
       // добавление "слушателей"
       gLDrawable.addKeyListener(this);
       gLDrawable.addMouseListener(this);
       gLDrawable.addMouseMotionListener(this);
       
   }
   
   /*================================================================*/
   /* Перерисовка при изменении размера окна */
   /*================================================================*/
   public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height) {
       final GL gl = gLDrawable.getGL();
       mainGUI.Mwidth = width;
       mainGUI.Mheigth = height;
       if(height <= 0) height = 1;
       final double h = (double)width / (double)height;
       gl.glMatrixMode(GL.GL_PROJECTION);
       gl.glLoadIdentity();
       glu.gluPerspective(50.0f, h, 1.0, 1000.0);
       gl.glMatrixMode(GL.GL_MODELVIEW);
       gl.glLoadIdentity();
   }
   
   /*================================================================*/
   /*Обработка нажатий клавиатуры*/
   /*================================================================*/
   public void keyPressed(KeyEvent e) {
       if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
           mainGUI.bQuit = true;
           mainGUI.displayT = null;
           System.exit(0);
       }
       if(e.getKeyCode() == KeyEvent.VK_ENTER) {
       }
       if(e.getKeyCode() == KeyEvent.VK_F1) {
    	    System.out.println("pressed <Help>");
    	    String message = "Help";
			JOptionPane.showMessageDialog(mainGUI.frame,message,"Помощь",JOptionPane.QUESTION_MESSAGE);
			 
       }
                   
       if(e.getKeyCode() == KeyEvent.VK_Q) movE_Z-=0.5f;
       if(e.getKeyCode() == KeyEvent.VK_A) movE_X-=0.5f;
       if(e.getKeyCode() == KeyEvent.VK_W) movE_Y+=0.5f;
       if(e.getKeyCode() == KeyEvent.VK_S) movE_Y-=0.5f;
       if(e.getKeyCode() == KeyEvent.VK_E) movE_Z+=0.5f;
       if(e.getKeyCode() == KeyEvent.VK_D) movE_X+=0.5f;
       
       if(e.getKeyCode() == KeyEvent.VK_Y) System.out.println("eye: " + movE_X + " " + movE_Y + " " + movE_Z);
   }

   public void keyReleased(KeyEvent e) {
   }

   public void keyTyped(KeyEvent e) {
   }
   
  
   /*================================================================*/
   /*Обработка нажатий мыши*/
   /*================================================================*/

	public void mouseClicked(MouseEvent e) {
		if ( e.getModifiers()==MouseEvent.BUTTON3_MASK && mouse_on_scene == 1) {
			mainGUI.bQuit = true;
	        mainGUI.displayT = null;
	        System.exit(0);
	    }
	}
	
	
	public void mouseEntered(MouseEvent e) {
		mouse_on_scene = 1;
	}
	
	public void mouseExited(MouseEvent e) {
		mouse_on_scene = 0;
	}
	
	public void mousePressed(MouseEvent e) {
	}
	
	public void mouseReleased(MouseEvent e) {
	}
	
	public void mouseDragged(MouseEvent e) {
		if ( e.getModifiersEx()==MouseEvent.BUTTON1_DOWN_MASK && mouse_on_scene == 1) {
			movE_X = (mainGUI.Mwidth/2-e.getX())/8;
			movE_Y = (mainGUI.Mheigth/2-e.getY())/8;
		}
	}
	
	public void mouseMoved(MouseEvent e) {
	}
	
	/*================================================================*/
	/* */
	/*================================================================*/
}