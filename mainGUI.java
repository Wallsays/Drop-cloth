package computing_mathematic;

import javax.media.opengl.GLCanvas;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.MenuShortcut;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


/** 14. Моделирование ткани, закрепленной в нескольких точках одной 
стороны прямоугольного участка с использованием метода 
частиц и обратного метода Эйлера **/

public class mainGUI implements Runnable {
	public static Thread displayT = new Thread(new mainGUI());
	public static boolean bQuit = false;
    public static JFrame frame;
    public static int Mwidth = 800,
    				  Mheigth = 600;
    public JLabel l1,l2,l3,l4,l5,l6,l7,l8,l9,l10,l11, l12,l13,l14, l15;
	public JTextField tf1,tf2,tf3,tf4,tf5,tf6,tf7,tf8,tf9,tf10, tf11,tf12,tf13, tf14;
	 	   JTextArea ta1;
	 	   JMenuBar menuBar;
	private boolean FullScr = false;
	private Renderer rend = new Renderer();
	private FPSmeter fps = new FPSmeter();
	private GLCanvas canvas;
	private Point loc;
	private Dimension screenSize;
	public int frameRatio; 
	long t0 = System.currentTimeMillis(), t1;
	
	GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice device = environment.getDefaultScreenDevice();
	
    public static void main(String[] args) {
    	displayT.start();
    }

    public void run() {
    	//JFrame.setDefaultLookAndFeelDecorated(true);
        frame = new JFrame("Моделирование падение ткани");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        canvas = new GLCanvas();
        canvas.addGLEventListener(rend); 
        canvas.addGLEventListener(fps);
        
        frame.getContentPane().add(canvas, BorderLayout.CENTER);
        //frame.setUndecorated(true);
        /*int size = frame.getExtendedState();
        size |= Frame.MAXIMIZED_VERT;//size |= Frame.MAXIMIZED_BOTH;
        frame.setExtendedState(size);*/
        
        /*================================================================*/
        /*Создание основных функциональных элементов в окне*/
        /*================================================================*/
        
        JPanel p = new JPanel();
		p.setLayout(new GridLayout(28, 2));
		l1 = new JLabel(" Параметры");
		p.add(l1); 
		l8 = new JLabel(" Количество точек по ширине(длине)");
		p.add(l8);
		tf7 = new JTextField(5);
		p.add(tf7);
		l9 = new JLabel(" Шаг сетки, см");
		p.add(l9);
		tf8 = new JTextField(5);
		p.add(tf8);
		l2 = new JLabel(" Масса, г");
		p.add(l2);
		tf1 = new JTextField(5);
		p.add(tf1);
		l5 = new JLabel(" Линейный коэффициент растяжения-сжатия, мН/см");
		p.add(l5);
		tf4 = new JTextField(5);
		p.add(tf4);
		l7 = new JLabel(" Линейный коэффициент изгиба, мН/см");
		p.add(l7);
		tf6 = new JTextField(5);
		p.add(tf6);
		l6 = new JLabel(" Линейный коэффициент сдвига, мН/см");
		p.add(l6);
		tf5 = new JTextField(5);
		p.add(tf5);
		l10 = new JLabel(" Расстояние до земли");
		//p.add(l10);
		tf9 = new JTextField(5);
		//p.add(tf9);
		l11 = new JLabel(" Количество закрепленных точек (5 макс)");
		p.add(l11);
		tf10 = new JTextField(5);
		p.add(tf10);
		
		l12 = new JLabel(" Константа характеризующая потери энергии, мг/с");
		p.add(l12);
		tf11 = new JTextField(5);
		p.add(tf11);
		l13 = new JLabel(" Шаг интегрирования");
		p.add(l13);
		tf12 = new JTextField(5);
		p.add(tf12);
		l14 = new JLabel(" Точность вычисления dv");
		p.add(l14);
		tf13 = new JTextField(5);
		p.add(tf13);
		l15 = new JLabel(" Максимальное количество инетраций");
		p.add(l15);
		tf14 = new JTextField(5);
		p.add(tf14);
		p.add(new JLabel(""));
		
		tf7.setText(Integer.toString(rend.sizeXxY));
		tf8.setText(Double.toString(rend.XstepX));
		
		tf4.setText(Double.toString(rend.ks));
		tf6.setText(Double.toString(rend.kb));
		tf5.setText(Double.toString(rend.kt));
		tf1.setText(Double.toString(rend.mass));
		tf10.setText(Integer.toString(rend.static_points));
		
		tf11.setText(Double.toString(rend.c));
		tf12.setText(Double.toString(rend.h));
		tf13.setText(Double.toString(rend.eps));
		tf14.setText(Integer.toString(rend.num_iteration));
		

		JButton button1 = new JButton("Обновить");
		button1.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					rend.sizeXxY = Integer.parseInt(tf7.getText());
					rend.XstepX = Double.parseDouble(tf8.getText());
					
					rend.ks = Double.parseDouble(tf4.getText());
					rend.kb = Double.parseDouble(tf6.getText());
					rend.kt = Double.parseDouble(tf5.getText());
					rend.mass = Double.parseDouble(tf1.getText());
					rend.mass1=rend.mass/(rend.sizeXxY*rend.sizeXxY);
					
					rend.static_points = Integer.parseInt(tf10.getText());
					
					rend.c = Double.parseDouble(tf11.getText());
					rend.h = Double.parseDouble(tf12.getText());
					rend.eps = Double.parseDouble(tf13.getText());
					rend.num_iteration=Integer.parseInt(tf14.getText());
					
					tf7.setText(Integer.toString(rend.sizeXxY));
					tf8.setText(Double.toString(rend.XstepX));
					
					tf4.setText(Double.toString(rend.ks));
					tf6.setText(Double.toString(rend.kb));
					tf5.setText(Double.toString(rend.kt));
					tf1.setText(Double.toString(rend.mass));
					
					tf10.setText(Integer.toString(rend.static_points));
					
					tf11.setText(Double.toString(rend.c));
					tf12.setText(Double.toString(rend.h));
					tf13.setText(Double.toString(rend.eps));
					tf14.setText(Integer.toString(rend.num_iteration));
					
					rend.start=0;
										
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(frame, "Некорректный ввод "+e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE); 
					System.out.println("Refresh Error:" + e);
				}
			}
		} );
		p.add(button1);
		JButton button2 = new JButton("Явный метод Эйлера");
		button2.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					
					rend.start=1;
					//frame.getContentPane().remove(menuBar);
					//getContentPane().add(menuBar, BorderLayout.NORTH);
					/*if (fps.FpS<40){
						rend.start=0;
						JOptionPane.showMessageDialog(frame, "Слишком много частиц", "Внимание", JOptionPane.INFORMATION_MESSAGE);
					}*/
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(frame, "Некорректные значения параметров"+e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE); 
					System.out.println("But2 Error:" + e);
				}
			}
		} );
		p.add(button2);
		JButton button3 = new JButton("Не явный метод Эйлера");
		button3.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					rend.start=2;
					/*if (fps.FpS<40){
						rend.start=0;
						JOptionPane.showMessageDialog(frame, "Слишком много частиц", "Внимание", JOptionPane.INFORMATION_MESSAGE);
					}*/
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(frame, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
					System.out.println("But3 Error:" + e);
				}
			}
		} );
		p.add(button3);
		JButton button4 = new JButton("Рунге-Кутт 4");
		button4.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					rend.start=3;
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(frame, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
					System.out.println("But3 Error:" + e);
				}
			}
		} );
		p.add(button4);
		JButton button5 = new JButton("Option ");
		button5.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					rend.start=4;
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(frame, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
					System.out.println("But3 Error:" + e);
				}
			}
		} );
		//p.add(button5);
		frame.getContentPane().add(p, BorderLayout.EAST);
		
	
		
		/*p = new JPanel();
		JTextArea ta1 = new JTextArea("Строка состояния...",1,20);
		ta1.setEditable(false);
		p.add(ta1);
		frame.getContentPane().add(p, BorderLayout.SOUTH);
		*/		
		
		
        /*================================================================*/
        /*Добавление полосы меню сверху*/
        /*================================================================*/
		
		Font font = new Font("Verdana", Font.PLAIN, 11);
		menuBar = new JMenuBar();
			
		JMenu fileMenu = new JMenu("Действия");
		fileMenu.setFont(font);
		
		JMenuItem PrefDotsItem = new JMenuItem("Оптимальная детализация");
		PrefDotsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2,Event.ALT_MASK));
		PrefDotsItem.setFont(font);
		//fileMenu.add(PrefDotsItem);
		
		JMenuItem fullscrItem = new JMenuItem("Полный экран");
		fullscrItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5,Event.ALT_MASK));
		fullscrItem.setFont(font);
		fileMenu.add(fullscrItem);
		
		fileMenu.addSeparator();
		
		JMenuItem exitItem = new JMenuItem("Выход");
		exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,Event.ALT_MASK));
		exitItem.setFont(font);
		fileMenu.add(exitItem);
		
		JMenu aboutMenu = new JMenu("Справка");
		aboutMenu.setFont(font);

		/*JMenuItem helpItem = new JMenuItem("Помощь"); 
		helpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1,Event.KEY_PRESS));
		MenuShortcut key1 = new MenuShortcut(KeyEvent.VK_F1);
		helpItem.setFont(font);
		aboutMenu.add(helpItem);*/
		
		JMenuItem aboutItem = new JMenuItem("О программе"); 
		aboutItem.setFont(font);
		aboutMenu.add(aboutItem);
		
		menuBar.add(fileMenu);
		menuBar.add(aboutMenu);
						
		frame.setJMenuBar(menuBar);
		//frame.getContentPane().add(menuBar, BorderLayout.NORTH);
		
		/*================================================================*/
		/*Добавление действий при нажатии на кнопки и пункты меню*/
		/*================================================================*/
				
				exitItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						System.out.println("pressed <EXIT>");
						System.exit(0);
					}
				});
				
				PrefDotsItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						System.out.println("pressed <PrefDots>");
						/*int i=10;
						while(fps.FpS>=70 || i<1000){
							rend.sizeXxY = i;
							tf7.setText(Integer.toString(rend.sizeXxY));
							i++;
						}*/
					}
				});
				
				fullscrItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						System.out.println("pressed <FULL SCREEN>");
						if (FullScr)FullScr=false;
						else FullScr=true;
						
						if(FullScr) {
							loc = frame.getLocation();
							screenSize = frame.getSize();
							Dimension MaxScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
							frame.setLocation(0,0);
							frame.setSize(MaxScreenSize);
							//frame.setUndecorated(true);
							frame.setAlwaysOnTop(true);
							frame.setResizable(false);
							}
						else{
							frame.setSize(screenSize);
							frame.setLocation(loc);
							frame.setAlwaysOnTop(false);
							frame.setResizable(true);
						}
					}
				});
				
				/*helpItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						System.out.println("pressed <Help> from mainGUI");
						String message = "Help";
						JOptionPane.showMessageDialog(frame,message,"Помощь",JOptionPane.QUESTION_MESSAGE);
						}
				});*/
				
				aboutItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						System.out.println("pressed <About Programm>");
						String message = "Курсовая работа по Вычислительной Математике\n" +
						 		    "\nМоделирование ткани, закрепленной в нескольких точках \nодной "+ 
						 			"стороны прямоугольного участка с использованием \nметода "+ 
						 			"частиц и обратного метода Эйлера" +
						 		    "\n\nВыполнил: Деревянко Денис АВТ-914 \n" + "                                    22.10.11 - 23.12.11 \n" ;
						JOptionPane.showMessageDialog(frame,message,"О программе",JOptionPane.INFORMATION_MESSAGE);
						}
				});
				
				
		/*================================================================*/
		/* Поведения окна*/
		/*================================================================*/
        
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                bQuit = true;
            }
            
        });
        
        /*================================================================*/
		/**/
		/*================================================================*/
        
        
        /*if (FullScr == false) { 
			frame.setSize(Mwidth, Mheigth);
			}
        else 	{
        	frame.setUndecorated(true);
        	int size = frame.getExtendedState();
        	size |= Frame.MAXIMIZED_BOTH;
        	frame.setExtendedState(size);
        }*/
        //while(displayT.isAlive()){frame.setUndecorated(FullScr);}
        frame.setSize(Mwidth, Mheigth);
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)screenSize.getWidth()/ 2;
		int centerY = (int)screenSize.getHeight()/2;
		frame.setLocation(centerX - frame.getWidth() / 2, centerY - frame.getHeight() / 2);
        frame.setVisible(true);
        canvas.requestFocus();
        
        while( !bQuit ) {
            canvas.display();
            t1 = System.currentTimeMillis();
            	/*if (fps.FpS<40 && (t1-t0)/1000>4){
				rend.start=0;
				JOptionPane.showMessageDialog(frame, "Слишком много частиц", "Внимание", JOptionPane.INFORMATION_MESSAGE);
				//System.out.println("te " + (t1-t1)/1000);
				rend.sizeXxY = 20;
				tf7.setText(Integer.toString(rend.sizeXxY));
				t0 = System.currentTimeMillis();
            	}*/
            }
    }
}