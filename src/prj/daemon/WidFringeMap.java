package prj.daemon;

public class WidFringeMap extends WidFringeView {

	public WidFringeMap() {
		super();
	}

	public WidFringeMap(int width, int height) {
		super(width, height);
	}
	// ----------------------------//

		
	public void drawMap(){
		//test drawing~~~
	}	
	
	/**
	 * produce Zernike term value
	 * @param n - Zernike number as given by James Wyant
	 * @param X - location X (normalized 0..1)
	 * @param Y - location X (normalized 0..1)
	 * @return term value
	 */
	public static double Zernike(int n, double X, double Y) {

		double X2 = 0., X3 = 0., X4 = 0.;
		double Y2 = 0., Y3 = 0., Y4 = 0.;
		double R2 = 0., R = 0.;
		//double last_x = 0.;
		//double last_y = 0.;
		//int cnt = 0;
		// if (last_x != X || last_y != Y)
		{
			X2 = X * X;
			X3 = X2 * X;
			X4 = X2 * X2;
			//last_x = X;
			Y2 = Y * Y;
			Y3 = Y2 * Y;
			Y4 = Y2 * Y2;
			R2 = X2 + Y2;
			R = Math.sqrt(R2);
			//last_y = Y;
		}

		double d;
		switch (n) {
		case 0:
			// return(.01 * sin(3 * M2PI * sqrt(R2)));
			return (1.);
		case 1:
			return (X);
		case 2:
			return (Y);
		case 3:
			return (-1. + 2. * R2);
		case 4:
			return (X2 - Y2);
		case 5:
			return (2. * X * Y);
		case 6:
			return (-2. * X + 3. * X * R2);
		case 7:
			return (-2. * Y + 3. * Y * R2);
		case 8:
			return (1. + R2 * (-6. + 6. * R2));
		case 9:
			return (X3 - 3. * X * Y2);
		case 10:
			return (3. * X2 * Y - Y3);
		case 11:
			return (-3. * X2 + 4. * X4 + 3. * Y2 - 4. * Y4);
		case 12:
			return 2. * X * Y * (-3. + 4. * R2);
		case 13:
			return X * (3. + R2 * (-12. + 10. * R2));
		case 14:
			return Y * (3. + R2 * (-12. + 10. * R2));
		case 15:
			return (-1. + R2 * (12. + R2 * (-30. + 20. * R2)));
		case 16:
			return (X4 - 6. * X2 * Y2 + Y4);
		case 17:
			return 4. * X * Y * (X2 - Y2);
		case 18:
			return X * (5. * X4 + 3. * Y2 * (4. - 5. * Y2) - 2 * X2 * (2. + 5. * Y2));
		case 19:
			return Y * (15. * X4 + 4. * Y2 - 5. * Y4 + 2. * X2 * (-6. + 5. * Y2));
		case 20:
			return (X2 - Y2) * (6. + R2 * (-20. + 15. * R2));
		case 21:
			return 2. * X * Y * (6. + R2 * (-20. + 15. * R2));
		case 22:
			return X * (-4. + R2 * (30. + R2 * (-60. + 35. * R2)));
		case 23:
			return Y * (-4. + R2 * (30. + R2 * (-60. + 35. * R2)));
		case 24:
			return (1. + R2 * (-20. + R2 * (90. + R2 * (-140. + 70. * R2))));
		case 25:
			return X * (X4 - 10. * X2 * Y2 + 5. * Y4);
		case 26:
			d = Y * (5. * X4 - 10. * X2 * Y2 + Y4);
			break;
		case 27:
			d = 6. * X4 * X2 - (30. * X2 * Y2) * (-1. + Y2) + Y4 * (-5. + 6. * Y2) - 5 * X4 * (1. + 6. * Y2);
			break;
		case 28:
			d = X * (-20. * X2 * Y + 20. * Y3 + 24. * X2 * Y * R2 - 24 * Y3 * R2);
			break;
		case 29:
			d = X * (10. * X2 - 30. * Y2 + R2 * (-30. * X2 + 90. * Y2 + R2 * (21. * X2 - 63. * Y2)));
			break;
		case 30:
			d = Y * (-10. * Y2 + 30. * X2 + R2 * (30. * Y2 - 90. * X2 + R2 * (-21. * Y2 + 63. * X2)));
			break;
		case 31:
			d = (-10. + R2 * (60. + R2 * (-105. + 56. * R2))) * (X2 - Y2);
			break;
		case 32:
			d = X * Y * (-20. + R2 * (120. + R2 * (-210. + 112. * R2)));
			break;
		case 33:
			d = X * (5. + R2 * (-60. + R2 * (210. + R2 * (-280. + R2 * 126))));
			break;
		case 34:
			d = Y * (5. + R2 * (-60. + R2 * (210. + R2 * (-280. + 126. * R2))));
			break;
		case 35:
			d = -1. + R2 * (30. + R2 * (-210. + R2 * (560. + R2 * (-630. + 252. * R2))));
			break;
		case 36:
			d = X4 * X2 - 15. * X4 * Y2 + 15. * X2 * Y4 - Y4 * Y2;
			break;
		case 37:
			d = 6. * X4 * X * Y - 20. * X3 * Y3 + 6. * X * Y4 * Y;
			break;
		case 38:
			d = -6. * X4 * X + 60. * X3 * Y2 - 30. * X * Y4 + 7. * X4 * X * R2 - 70. * X3 * Y2 * R2 + 35. * X * Y4 * R2;
			break;
		case 39: // Spherical 5
			d = 1. + R2 * (-42. + R2 * (420. + R2 * (-1680. + R2 * (3150. + R2 * (-2772. + 924. * R2)))));
			break;
		case 40: // spherical 6
			d = -1. + R2 * (56.
					+ R2 * (-756. + R2 * (4200. + R2 * (-11550. + R2 * (16632. + R2 * (-12012. + 3432. * R2))))));
			break;
		case 41: // spherical 7
			d = 1. + R2 * (-72. + R2 * (1260.
					+ R2 * (-9240. + R2 * (34650. + R2 * (-72072. + R2 * (84084. + R2 * (-51480. + 12870. * R2)))))));
			// d = zpr(8,0,R);
			break;
		default:
			return (0.);
		}
		return d;
	}
}
