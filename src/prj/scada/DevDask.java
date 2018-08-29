package prj.scada;

import narl.itrc.DevBase;

abstract class DevDask extends DevBase {
	
	protected int card = -1;
	
	public DevDask(String tag) {
		super(tag);
	}

	/*
	 * ADLink PCI Card Type
	 */
	protected static final int PCI_6208V = 1;
	protected static final int PCI_6208A = 2;
	protected static final int PCI_6308V = 3;
	protected static final int PCI_6308A = 4;
	protected static final int PCI_7200 = 5;
	protected static final int PCI_7230 = 6;
	protected static final int PCI_7233 = 7;
	protected static final int PCI_7234 = 8;
	protected static final int PCI_7248 = 9;
	protected static final int PCI_7249 = 10;
	protected static final int PCI_7250 = 11;
	protected static final int PCI_7252 = 12;
	protected static final int PCI_7296 = 13;
	protected static final int PCI_7300A_RevA = 14;
	protected static final int PCI_7300A_RevB = 15;
	protected static final int PCI_7432 = 16;
	protected static final int PCI_7433 = 17;
	protected static final int PCI_7434 = 18;
	protected static final int PCI_8554 = 19;
	protected static final int PCI_9111DG = 20;
	protected static final int PCI_9111HR = 21;
	protected static final int PCI_9112 = 22;
	protected static final int PCI_9113 = 23;
	protected static final int PCI_9114DG = 24;
	protected static final int PCI_9114HG = 25;
	protected static final int PCI_9118DG = 26;
	protected static final int PCI_9118HG = 27;
	protected static final int PCI_9118HR = 28;
	protected static final int PCI_9810 = 29;
	protected static final int PCI_9812 = 30;
	protected static final int PCI_7396 = 31;
	protected static final int PCI_9116 = 32;
	protected static final int PCI_7256 = 33;
	protected static final int PCI_7258 = 34;
	protected static final int PCI_7260 = 35;
	protected static final int PCI_7452 = 36;
	protected static final int PCI_7442 = 37;
	protected static final int PCI_7443 = 38;
	protected static final int PCI_7444 = 39;
	protected static final int PCI_9221 = 40;
	protected static final int PCI_9524 = 41;
	protected static final int PCI_6202 = 42;
	protected static final int PCI_9222 = 43;
	protected static final int PCI_9223 = 44;
	protected static final int PCI_7433C = 45;
	protected static final int PCI_7434C = 46;
	protected static final int PCI_922A = 47;
	protected static final int PCI_7350 = 48;
	protected static final int PCI_7360 = 49;
	protected static final int PCI_7300A_RevC = 50;
	
	protected static final int MAX_CARD = 32;
	
	/*
	 * Error Number
	 */
	protected static final int NoError = 0;
	protected static final int ErrorUnknownCardType = -1;
	protected static final int ErrorInvalidCardNumber = -2;
	protected static final int ErrorTooManyCardRegistered = -3;
	protected static final int ErrorCardNotRegistered = -4;
	protected static final int ErrorFuncNotSupport = -5;
	protected static final int ErrorInvalidIoChannel = -6;
	protected static final int ErrorInvalidAdRange = -7;
	protected static final int ErrorContIoNotAllowed = -8;
	protected static final int ErrorDiffRangeNotSupport = -9;
	protected static final int ErrorLastChannelNotZero = -10;
	protected static final int ErrorChannelNotDescending = -11;
	protected static final int ErrorChannelNotAscending = -12;
	protected static final int ErrorOpenDriverFailed = -13;
	protected static final int ErrorOpenEventFailed = -14;
	protected static final int ErrorTransferCountTooLarge = -15;
	protected static final int ErrorNotDoubleBufferMode = -16;
	protected static final int ErrorInvalidSampleRate = -17;
	protected static final int ErrorInvalidCounterMode = -18;
	protected static final int ErrorInvalidCounter = -19;
	protected static final int ErrorInvalidCounterState = -20;
	protected static final int ErrorInvalidBinBcdParam = -21;
	protected static final int ErrorBadCardType = -22;
	protected static final int ErrorInvalidDaRefVoltage = -23;
	protected static final int ErrorAdTimeOut = -24;
	protected static final int ErrorNoAsyncAI = -25;
	protected static final int ErrorNoAsyncAO = -26;
	protected static final int ErrorNoAsyncDI = -27;
	protected static final int ErrorNoAsyncDO = -28;
	protected static final int ErrorNotInputPort = -29;
	protected static final int ErrorNotOutputPort = -30;
	protected static final int ErrorInvalidDioPort = -31;
	protected static final int ErrorInvalidDioLine = -32;
	protected static final int ErrorContIoActive = -33;
	protected static final int ErrorDblBufModeNotAllowed = -34;
	protected static final int ErrorConfigFailed = -35;
	protected static final int ErrorInvalidPortDirection = -36;
	protected static final int ErrorBeginThreadError = -37;
	protected static final int ErrorInvalidPortWidth = -38;
	protected static final int ErrorInvalidCtrSource = -39;
	protected static final int ErrorOpenFile = -40;
	protected static final int ErrorAllocateMemory = -41;
	protected static final int ErrorDaVoltageOutOfRange = -42;
	protected static final int ErrorDaExtRefNotAllowed = -43;
	protected static final int ErrorDIODataWidthError = -44;
	protected static final int ErrorTaskCodeError = -45;
	protected static final int ErrortriggercountError = -46;
	protected static final int ErrorInvalidTriggerMode = -47;
	protected static final int ErrorInvalidTriggerType = -48;
	protected static final int ErrorInvalidCounterValue = -50;
	protected static final int ErrorInvalidEventHandle = -60;
	protected static final int ErrorNoMessageAvailable = -61;
	protected static final int ErrorEventMessgaeNotAdded = -62;
	protected static final int ErrorCalibrationTimeOut = -63;
	protected static final int ErrorUndefinedParameter = -64;
	protected static final int ErrorInvalidBufferID = -65;
	protected static final int ErrorInvalidSampledClock = -66;
	protected static final int ErrorInvalidOperationMode = -67;
	/*Error number for driver API*/
	protected static final int ErrorConfigIoctl = -201;
	protected static final int ErrorAsyncSetIoctl = -202;
	protected static final int ErrorDBSetIoctl = -203;
	protected static final int ErrorDBHalfReadyIoctl = -204;
	protected static final int ErrorContOPIoctl = -205;
	protected static final int ErrorContStatusIoctl = -206;
	protected static final int ErrorPIOIoctl = -207;
	protected static final int ErrorDIntSetIoctl = -208;
	protected static final int ErrorWaitEvtIoctl = -209;
	protected static final int ErrorOpenEvtIoctl = -210;
	protected static final int ErrorCOSIntSetIoctl = -211;
	protected static final int ErrorMemMapIoctl = -212;
	protected static final int ErrorMemUMapSetIoctl = -213;
	protected static final int ErrorCTRIoctl = -214;
	protected static final int ErrorGetResIoctl = -215;
	protected static final int ErrorCalIoctl = -216;
	protected static final int ErrorPMIntSetIoctl = -217;
	
	/*
	 * AD Range
	 */
	protected static final int AD_B_10_V = 1;
	protected static final int AD_B_5_V = 2;
	protected static final int AD_B_2_5_V = 3;
	protected static final int AD_B_1_25_V = 4;
	protected static final int AD_B_0_625_V = 5;
	protected static final int AD_B_0_3125_V = 6;
	protected static final int AD_B_0_5_V = 7;
	protected static final int AD_B_0_05_V = 8;
	protected static final int AD_B_0_005_V = 9;
	protected static final int AD_B_1_V = 10;
	protected static final int AD_B_0_1_V = 11;
	protected static final int AD_B_0_01_V = 12;
	protected static final int AD_B_0_001_V = 13;
	protected static final int AD_U_20_V = 14;
	protected static final int AD_U_10_V = 15;
	protected static final int AD_U_5_V = 16;
	protected static final int AD_U_2_5_V = 17;
	protected static final int AD_U_1_25_V = 18;
	protected static final int AD_U_1_V = 19;
	protected static final int AD_U_0_1_V = 20;
	protected static final int AD_U_0_01_V = 21;
	protected static final int AD_U_0_001_V = 22;
	protected static final int AD_B_2_V = 23;
	protected static final int AD_B_0_25_V = 24;
	protected static final int AD_B_0_2_V = 25;
	protected static final int AD_U_4_V = 26;
	protected static final int AD_U_2_V = 27;
	protected static final int AD_U_0_5_V = 28;
	protected static final int AD_U_0_4_V = 29;
	
	/*------------------*/
	/* Common Constants */
	/*------------------*/
	/* T or F*/
	protected static final int TRUE = 1;
	protected static final int FALSE = 0;
	
	/*Synchronous Mode*/
	protected static final int SYNCH_OP = 1;
	protected static final int ASYNCH_OP = 2;
	
	/*AO Terminate Mode*/
	protected static final int DA_TerminateImmediate = 0;
	
	/*DIO Port Direction*/
	protected static final int INPUT_PORT = 1;
	protected static final int OUTPUT_PORT = 2;
	/*DIO Line Direction*/
	protected static final int INPUT_LINE = 1;
	protected static final int OUTPUT_LINE = 2;
	
	/*Clock Mode*/
	protected static final int TRIG_SOFTWARE = 0;
	protected static final int TRIG_INT_PACER = 1;
	protected static final int TRIG_EXT_STROBE = 2;
	protected static final int TRIG_HANDSHAKE = 3;
	protected static final int TRIG_CLK_10MHZ = 4;
	protected static final int TRIG_CLK_20MHZ = 5;
	protected static final int TRIG_DOCLK_TIMER_ACK = 6;
	protected static final int TRIG_DOCLK_10M_ACK = 7;
	protected static final int TRIG_DOCLK_20M_ACK = 8;
	
	/*Virtual Sampling Rate for using external clock as the clock source*/
	protected static final int CLKSRC_EXT_SampRate = 10000;
	
	/*Register by slot*/
	protected static final int RegBySlot = 0x8000;
	
	/*PCI info*/
	protected static int PCILocInfo(int bus, int dev, int fun){
		return RegBySlot|((bus&0xffff)<<8)|((dev&0x1f)<<3)|(fun&0x7);
	}

	/*DIO & AFI Voltage Level*/
	protected static final int VoltLevel_3R3 = 0;
	protected static final int VoltLevel_2R5 = 1;
	protected static final int VoltLevel_1R8 = 2;
	protected static final int VoltLevel_DisableDO = 3;
	
	/*---------------------------------------------*/
	/* Constants for PCI-6208A/PCI-6308A/PCI-6308V */
	/*---------------------------------------------*/
	/*Output Mode*/
	protected static final int P6208_CURRENT_0_20MA = 0;
	protected static final int P6208_CURRENT_4_20MA = 3;
	protected static final int P6208_CURRENT_5_25MA = 1;
	protected static final int P6308_CURRENT_0_20MA = 0;
	protected static final int P6308_CURRENT_4_20MA = 3;
	protected static final int P6308_CURRENT_5_25MA = 1;
	/*AO Setting*/
	protected static final int P6308V_AOCH0_3 = 0;
	protected static final int P6308V_AOCH4_7 = 1;
	protected static final int P6308V_AOUNIPOLAR = 0;
	protected static final int P6308V_AOBIPOLAR = 1;
	
	
	/*------------------------*/
	/* Constants for PCI-7200 */
	/*------------------------*/
	/*InputMode*/
	protected static final int DIWAITING = 0x02;
	protected static final int DINOWAITING = 0x00;
	protected static final int DITRIG_RISING = 0x04;
	protected static final int DITRIG_FALLING = 0x00;
	protected static final int IREQ_RISING = 0x08;
	protected static final int IREQ_FALLING = 0x00;
	/*Output Mode*/
	protected static final int OREQ_ENABLE = 0x10;
	protected static final int OREQ_DISABLE = 0x00;
	protected static final int OTRIG_HIGH = 0x20;
	protected static final int OTRIG_LOW = 0x00;
	
	
	/*----------------------------------*/
	/* Constants for PCI-7248/7296/7396 */
	/*----------------------------------*/
	/*Channel & Port*/
	protected static final int Channel_P1A = 0;
	protected static final int Channel_P1B = 1;
	protected static final int Channel_P1C = 2;
	protected static final int Channel_P1CL = 3;
	protected static final int Channel_P1CH = 4;
	protected static final int Channel_P1AE = 10;
	protected static final int Channel_P1BE = 11;
	protected static final int Channel_P1CE = 12;
	protected static final int Channel_P2A = 5;
	protected static final int Channel_P2B = 6;
	protected static final int Channel_P2C = 7;
	protected static final int Channel_P2CL = 8;
	protected static final int Channel_P2CH = 9;
	protected static final int Channel_P2AE = 15;
	protected static final int Channel_P2BE = 16;
	protected static final int Channel_P2CE = 17;
	protected static final int Channel_P3A = 10;
	protected static final int Channel_P3B = 11;
	protected static final int Channel_P3C = 12;
	protected static final int Channel_P3CL = 13;
	protected static final int Channel_P3CH = 14;
	protected static final int Channel_P4A = 15;
	protected static final int Channel_P4B = 16;
	protected static final int Channel_P4C = 17;
	protected static final int Channel_P4CL = 18;
	protected static final int Channel_P4CH = 19;
	protected static final int Channel_P5A = 20;
	protected static final int Channel_P5B = 21;
	protected static final int Channel_P5C = 22;
	protected static final int Channel_P5CL = 23;
	protected static final int Channel_P5CH = 24;
	protected static final int Channel_P6A = 25;
	protected static final int Channel_P6B = 26;
	protected static final int Channel_P6C = 27;
	protected static final int Channel_P6CL = 28;
	protected static final int Channel_P6CH = 29;
	/*the following are used for PCI7396*/
	protected static final int Channel_P1 = 30;
	protected static final int Channel_P2 = 31;
	protected static final int Channel_P3 = 32;
	protected static final int Channel_P4 = 33;
	protected static final int Channel_P1E = 34;
	protected static final int Channel_P2E = 35;
	protected static final int Channel_P3E = 36;
	protected static final int Channel_P4E = 37;
	
	
	/*----------------------------------*/
	/* Constants for PCI-7442/7443/7444 */
	/*----------------------------------*/
	/*P7442*/
	protected static final int P7442_CH0 = 0;
	protected static final int P7442_CH1 = 1;
	protected static final int P7442_TTL0 = 2;
	protected static final int P7442_TTL1 = 3;
	/*P7443*/
	protected static final int P7443_CH0 = 0;
	protected static final int P7443_CH1 = 1;
	protected static final int P7443_CH2 = 2;
	protected static final int P7443_CH3 = 3;
	protected static final int P7443_TTL0 = 4;
	protected static final int P7443_TTL1 = 5;
	/*P7444*/
	protected static final int P7444_CH0 = 0;
	protected static final int P7444_CH1 = 1;
	protected static final int P7444_CH2 = 2;
	protected static final int P7444_CH3 = 3;
	protected static final int P7444_TTL0 = 4;
	protected static final int P7444_TTL1 = 5;
	
	/*COS Counter OP*/
	protected static final int COS_COUNTER_RESET = 0;
	protected static final int COS_COUNTER_SETUP = 1;
	protected static final int COS_COUNTER_START = 2;
	protected static final int COS_COUNTER_STOP = 3;
	protected static final int COS_COUNTER_READ = 4;
	
	
	/*
	 * EMG shdn ctrl code
	 */
	protected static final int EMGSHDN_OFF = 0;
	protected static final int EMGSHDN_ON = 1;
	protected static final int EMGSHDN_RECOVERY = 2;
	
	
	/*
	 * Hot Reset Hold ctrl code
	 */
	protected static final int HRH_OFF = 0;
	protected static final int HRH_ON = 1;
	
	
	/*-------------------------*/
	/* Constants for PCI-7300A */
	/*-------------------------*/
	/*Wait Status*/
	protected static final int P7300_WAIT_NO = 0;
	protected static final int P7300_WAIT_TRG = 1;
	protected static final int P7300_WAIT_FIFO = 2;
	protected static final int P7300_WAIT_BOTH = 3;
	
	/*Terminator control*/
	protected static final int P7300_TERM_OFF = 0;
	protected static final int P7300_TERM_ON = 1;
	
	/*DI control signals polarity for PCI-7300A Rev. B*/
	protected static final int P7300_DIREQ_POS = 0x00000000;
	protected static final int P7300_DIREQ_NEG = 0x00000001;
	protected static final int P7300_DIACK_POS = 0x00000000;
	protected static final int P7300_DIACK_NEG = 0x00000002;
	protected static final int P7300_DITRIG_POS = 0x00000000;
	protected static final int P7300_DITRIG_NEG = 0x00000004;
	/*DO control signals polarity for PCI-7300A Rev. B*/
	protected static final int P7300_DOREQ_POS = 0x00000000;
	protected static final int P7300_DOREQ_NEG = 0x00000008;
	protected static final int P7300_DOACK_POS = 0x00000000;
	protected static final int P7300_DOACK_NEG = 0x00000010;
	protected static final int P7300_DOTRIG_POS = 0x00000000;
	protected static final int P7300_DOTRIG_NEG = 0x00000020;
	
	/*DO Disable mode in DOAsyncClear*/
	protected static final int P7300_DODisableDOEnabled = 0;
	protected static final int P7300_DONotDisableDOEnabled = 1;
	
	
	/*----------------------------------------------*/
	/* Constants for PCI-7432/7433/7434/7433C/7434C */
	/*----------------------------------------------*/
	protected static final int PORT_DILOW = 0;
	protected static final int PORT_DIHIGH = 1;
	protected static final int PORT_DOLOW = 0;
	protected static final int PORT_DOHIGH = 1;
	protected static final int P7432R_DOLED = 1;
	protected static final int P7433R_DOLED = 0;
	protected static final int P7434R_DOLED = 2;
	protected static final int P7432R_DISLOT = 1;
	protected static final int P7433R_DISLOT = 2;
	protected static final int P7434R_DISLOT = 0;
	
	
	/*----------------------------------------------------------------------------*/
	/* Dual-Interrupt Source control for PCI-7248/96 & 7432/33 & 7230 & 8554 &    */
	/* 7396 & 7256 & 7260 & 7442/43/44 & 7433C                                    */
	/*----------------------------------------------------------------------------*/
	protected static final int INT1_NC = -2;
	protected static final int INT1_DISABLE = -1;
	protected static final int INT1_COS = 0;
	protected static final int INT1_FP1C0 = 1;
	protected static final int INT1_RP1C0_FP1C3 = 2;
	protected static final int INT1_EVENT_COUNTER = 3;
	protected static final int INT1_EXT_SIGNAL = 1;
	protected static final int INT1_COUT12 = 1;
	protected static final int INT1_CH0 = 1;
	protected static final int INT1_COS0 = 1;
	protected static final int INT1_COS1 = 2;
	protected static final int INT1_COS2 = 4;
	protected static final int INT1_COS3 = 8;
	protected static final int INT2_NC = -2;
	protected static final int INT2_DISABLE = -1;
	protected static final int INT2_COS = 0;
	protected static final int INT2_FP2C0 = 1;
	protected static final int INT2_RP2C0_FP2C3 = 2;
	protected static final int INT2_TIMER_COUNTER = 3;
	protected static final int INT2_EXT_SIGNAL = 1;
	protected static final int INT2_CH1 = 2;
	protected static final int INT2_WDT = 4;
	protected static final int ManualResetIEvt = 0x4000;
	protected static final int WDTOVRFLOW_SAFETYOUT = 0x8000;
	
	
	/*------------------------*/
	/* Constants for PCI-8554 */
	/*------------------------*/
	/*Clock Source of Cunter N*/
	protected static final int ECKN = 0;
	protected static final int COUTN_1 = 1;
	protected static final int CK1 = 2;
	protected static final int COUT10 = 3;
	/*Clock Source of CK1*/
	protected static final int CK1_C8M = 0;
	protected static final int CK1_COUT11 = 1;
	/*Debounce Clock*/
	protected static final int DBCLK_COUT11 = 0;
	protected static final int DBCLK_2MHZ = 1;
	
	
	/*------------------------*/
	/* Constants for PCI-9111 */
	/*------------------------*/
	/*Dual Interrupt Mode*/
	protected static final int P9111_INT1_EOC = 0;
	protected static final int P9111_INT1_FIFO_HF = 1;
	protected static final int P9111_INT2_PACER = 0;
	protected static final int P9111_INT2_EXT_TRG = 1;
	/*Channel Count*/
	protected static final int P9111_CHANNEL_DO = 0;
	protected static final int P9111_CHANNEL_EDO = 1;
	protected static final int P9111_CHANNEL_DI = 0;
	protected static final int P9111_CHANNEL_EDI = 1;
	/*EDO function*/
	protected static final int P9111_EDOINPUT = 1;
	protected static final int P9111_EDOOUT_EDO = 2;
	protected static final int P9111_EDOOUT_CHN = 3;
	/*Trigger Mode*/
	protected static final int P9111_TRGMOD_SOFT = 0x00;
	protected static final int P9111_TRGMOD_PRE = 0x01;
	protected static final int P9111_TRGMOD_POST = 0x02;
	/*AO Setting*/
	protected static final int P9111_AOUNIPOLAR = 0;
	protected static final int P9111_AOBIPOLAR = 1;
	
	
	/*------------------------*/
	/* Constants for PCI-9118 */
	/*------------------------*/
	protected static final int P9118_AIBiPolar = 0x00;
	protected static final int P9118_AIUniPolar = 0x01;
	
	protected static final int P9118_AISingEnded = 0x00;
	protected static final int P9118_AIDifferential = 0x02;
	
	protected static final int P9118_AIExtG = 0x04;
	
	protected static final int P9118_AIExtTrig = 0x08;
	
	protected static final int P9118_AIDtrgNegative = 0x00;
	protected static final int P9118_AIDtrgPositive = 0x10;
	
	protected static final int P9118_AIEtrgNegative = 0x00;
	protected static final int P9118_AIEtrgPositive = 0x20;
	
	protected static final int P9118_AIBurstModeEn = 0x40;
	protected static final int P9118_AISampleHold = 0x80;
	protected static final int P9118_AIPostTrgEn = 0x100;
	protected static final int P9118_AIAboutTrgEn = 0x200;
	
	
	/*------------------------*/
	/* Constants for PCI-9116 */
	/*------------------------*/
	protected static final int P9116_AILocalGND = 0x00;
	protected static final int P9116_AIUserCMMD = 0x01;
	protected static final int P9116_AISingEnded = 0x00;
	protected static final int P9116_AIDifferential = 0x02;
	protected static final int P9116_AIBiPolar = 0x00;
	protected static final int P9116_AIUniPolar = 0x04;
	
	protected static final int P9116_TRGMOD_SOFT = 0x00;
	protected static final int P9116_TRGMOD_POST = 0x10;
	protected static final int P9116_TRGMOD_DELAY = 0x20;
	protected static final int P9116_TRGMOD_PRE = 0x30;
	protected static final int P9116_TRGMOD_MIDL = 0x40;
	protected static final int P9116_AITrgPositive = 0x00;
	protected static final int P9116_AITrgNegative = 0x80;
	protected static final int P9116_AIExtTimeBase = 0x100;
	protected static final int P9116_AIIntTimeBase = 0x000;
	protected static final int P9116_AIDlyInSamples = 0x200;
	protected static final int P9116_AIDlyInTimebase = 0x000;
	protected static final int P9116_AIReTrigEn = 0x400;
	protected static final int P9116_AIMCounterEn = 0x800;
	protected static final int P9116_AISoftPolling = 0x0000;
	protected static final int P9116_AIINT = 0x1000;
	protected static final int P9116_AIDMA = 0x2000;
	
	
	/*------------------------*/
	/* Constants for PCI-9812 */
	/*------------------------*/
	/*Trigger Mode*/
	protected static final int P9812_TRGMOD_SOFT = 0x00;
	protected static final int P9812_TRGMOD_POST = 0x01;
	protected static final int P9812_TRGMOD_PRE = 0x02;
	protected static final int P9812_TRGMOD_DELAY = 0x03;
	protected static final int P9812_TRGMOD_MIDL = 0x04;
	
	protected static final int P9812_AIEvent_Manual = 0x80;
	
	/*Trigger Source*/
	protected static final int P9812_TRGSRC_CH0 = 0x00;
	protected static final int P9812_TRGSRC_CH1 = 0x08;
	protected static final int P9812_TRGSRC_CH2 = 0x10;
	protected static final int P9812_TRGSRC_CH3 = 0x18;
	protected static final int P9812_TRGSRC_EXT_DIG = 0x20;
	
	/*Trigger Polarity*/
	protected static final int P9812_TRGSLP_POS = 0x00;
	protected static final int P9812_TRGSLP_NEG = 0x40;
	
	/*Frequency Selection*/
	protected static final int P9812_AD2_GT_PCI = 0x80;
	protected static final int P9812_AD2_LT_PCI = 0x00;
	
	/*Clock Source*/
	protected static final int P9812_CLKSRC_INT = 0x000;
	protected static final int P9812_CLKSRC_EXT_SIN = 0x100;
	protected static final int P9812_CLKSRC_EXT_DIG = 0x200;
	
	
	/*------------------------*/
	/* Constants for PCI-9221 */
	/*------------------------*/
	/*Input Type*/
	protected static final int P9221_AISingEnded = 0x0;
	protected static final int P9221_AINonRef_SingEnded = 0x1;
	protected static final int P9221_AIDifferential = 0x2;
	
	/*Trigger Mode*/
	protected static final int P9221_TRGMOD_SOFT = 0x00;
	protected static final int P9221_TRGMOD_ExtD = 0x08;
	/*Trigger Source*/
	protected static final int P9221_TRGSRC_GPI0 = 0x00;
	protected static final int P9221_TRGSRC_GPI1 = 0x01;
	protected static final int P9221_TRGSRC_GPI2 = 0x02;
	protected static final int P9221_TRGSRC_GPI3 = 0x03;
	protected static final int P9221_TRGSRC_GPI4 = 0x04;
	protected static final int P9221_TRGSRC_GPI5 = 0x05;
	protected static final int P9221_TRGSRC_GPI6 = 0x06;
	protected static final int P9221_TRGSRC_GPI7 = 0x07;
	/*Trigger Polarity*/
	protected static final int P9221_AITrgPositive = 0x00;
	protected static final int P9221_AITrgNegative = 0x10;
	
	/*TimeBase Mode*/
	protected static final int P9221_AIIntTimeBase = 0x00;
	protected static final int P9221_AIExtTimeBase = 0x80;
	/*TimeBase Source*/
	protected static final int P9221_TimeBaseSRC_GPI0 = 0x00;
	protected static final int P9221_TimeBaseSRC_GPI1 = 0x10;
	protected static final int P9221_TimeBaseSRC_GPI2 = 0x20;
	protected static final int P9221_TimeBaseSRC_GPI3 = 0x30;
	protected static final int P9221_TimeBaseSRC_GPI4 = 0x40;
	protected static final int P9221_TimeBaseSRC_GPI5 = 0x50;
	protected static final int P9221_TimeBaseSRC_GPI6 = 0x60;
	protected static final int P9221_TimeBaseSRC_GPI7 = 0x70;
	
	/*EEPROM*/
	protected static final int EEPROM_DEFAULT_BANK = 0;
	protected static final int EEPROM_USER_BANK1 = 1;
	
	
	/*---------------*/
	/* Timer/Counter */
	/*---------------*/
	/*Counter Mode(8254)*/
	protected static final int TOGGLE_OUTPUT = 0;
	protected static final int PROG_ONE_SHOT = 1;
	protected static final int RATE_GENERATOR = 2;
	protected static final int SQ_WAVE_RATE_GENERATOR = 3;
	protected static final int SOFT_TRIG = 4;
	protected static final int HARD_TRIG = 5;
	
	/*16-bit binary or 4-decade BCD counter*/
	protected static final int BIN = 0;
	protected static final int BCD = 1;
	
	
	/*-------------------------------*/
	/* General Purpose Timer/Counter */
	/*-------------------------------*/
	/*Counter Mode*/
	protected static final int General_Counter = 0x00;
	protected static final int Pulse_Generation = 0x01;
	/*GPTC clock source*/
	protected static final int GPTCCLKSRC_EXT = 0x08;
	protected static final int GPTCCLKSRC_INT = 0x00;
	protected static final int GPTCGATESRC_EXT = 0x10;
	protected static final int GPTCGATESRC_INT = 0x00;
	protected static final int GPTCUPDOWN_SELECT_EXT = 0x20;
	protected static final int GPTCUPDOWN_SELECT_SOFT = 0x00;
	protected static final int GPTCUP_CTR = 0x40;
	protected static final int GPTCDOWN_CTR = 0x00;
	protected static final int GPTCENABLE = 0x80;
	protected static final int GPTCDISABLE = 0x00;
	
	
	/*-------------------------------------------------*/
	/* General Purpose Timer/Counter for PCI-922x/6202 */
	/*-------------------------------------------------*/
	/*Counter Mode*/
	protected static final int SimpleGatedEventCNT = 0x01;
	protected static final int SinglePeriodMSR = 0x02;
	protected static final int SinglePulseWidthMSR = 0x03;
	protected static final int SingleGatedPulseGen = 0x04;
	protected static final int SingleTrigPulseGen = 0x05;
	protected static final int RetrigSinglePulseGen = 0x06;
	protected static final int SingleTrigContPulseGen = 0x07;
	protected static final int ContGatedPulseGen = 0x08;
	protected static final int EdgeSeparationMSR = 0x09;
	protected static final int SingleTrigContPulseGenPWM = 0x0a;
	protected static final int ContGatedPulseGenPWM = 0x0b;
	protected static final int CW_CCW_Encoder = 0x0c;
	protected static final int x1_AB_Phase_Encoder = 0x0d;
	protected static final int x2_AB_Phase_Encoder = 0x0e;
	protected static final int x4_AB_Phase_Encoder = 0x0f;
	protected static final int Phase_Z = 0x10;
	protected static final int OnFlyChange_PulseCounters = 0x81;
	/*GPTC clock source*/
	protected static final int GPTCCLK_SRC_Ext = 0x01;
	protected static final int GPTCCLK_SRC_Int = 0x00;
	protected static final int GPTCGATE_SRC_Ext = 0x02;
	protected static final int GPTCGATE_SRC_Int = 0x00;
	protected static final int GPTCUPDOWN_Ext = 0x04;
	protected static final int GPTCUPDOWN_Int = 0x00;
	/*GPTC clock polarity*/
	protected static final int GPTCCLKSRC_LACTIVE = 0x01;
	protected static final int GPTCCLKSRC_HACTIVE = 0x00;
	protected static final int GPTCGATE_LACTIVE = 0x02;
	protected static final int GPTCGATE_HACTIVE = 0x00;
	protected static final int GPTCUPDOWN_LACTIVE = 0x04;
	protected static final int GPTCUPDOWN_HACTIVE = 0x00;
	protected static final int GPTCOUTPUT_LACTIVE = 0x08;
	protected static final int GPTCOUTPUT_HACTIVE = 0x00;
	/*GPTC OP Parameter*/
	protected static final int IntGate = 0x0;
	protected static final int IntUpDnCTR = 0x1;
	protected static final int IntENABLE = 0x2;
	/*Z-Phase*/
	protected static final int GPTCEZ0_ClearPhase0 = 0x0;
	protected static final int GPTCEZ0_ClearPhase1 = 0x1;
	protected static final int GPTCEZ0_ClearPhase2 = 0x2;
	protected static final int GPTCEZ0_ClearPhase3 = 0x3;
	/*Z-Mode*/
	protected static final int GPTCEZ0_ClearMode0 = 0x0;
	protected static final int GPTCEZ0_ClearMode1 = 0x1;
	protected static final int GPTCEZ0_ClearMode2 = 0x2;
	protected static final int GPTCEZ0_clearMode3 = 0x3;
	
	
	/*----------------*/
	/* Watchdog Timer */
	/*----------------*/
	/*Counter action*/
	protected static final int WDTDISARM = 0;
	protected static final int WDTARM = 1;
	protected static final int WDTRESTART = 2;
	/*Pattern ID*/
	protected static final int INIT_PTN = 0;
	protected static final int EMGSHDN_PTN = 1;
	/*Pattern ID for 7442/7444*/
	protected static final int INIT_PTN_CH0 = 0;
	protected static final int INIT_PTN_CH1 = 1;
	protected static final int INIT_PTN_CH2 = 2;
	protected static final int INIT_PTN_CH3 = 3;
	protected static final int SAFTOUT_PTN_CH0 = 4;
	protected static final int SAFTOUT_PTN_CH1 = 5;
	protected static final int SAFTOUT_PTN_CH2 = 6;
	protected static final int SAFTOUT_PTN_CH3 = 7;
	
	/*--------------------------------------*/
	/* DAQ Event type for the event message */
	/*--------------------------------------*/
	protected static final int AIEnd = 0;
	protected static final int AOEnd = 0;
	protected static final int DIEnd = 0;
	protected static final int DOEnd = 0;
	protected static final int DBEvent = 1;
	protected static final int TrigEvent = 2;
	
	
	/*------------------------*/
	/* Constants for PCI-9524 */
	/*------------------------*/
	/*AI Interrupt*/
	protected static final int P9524_INT_LC_EOC = 0x2;
	protected static final int P9524_INT_GP_EOC = 0x3;
	/*DSP Constants*/
	protected static final int P9524_SPIKE_REJ_DISABLE = 0x0;
	protected static final int P9524_SPIKE_REJ_ENABLE = 0x1;
	/*Transfer Mode*/
	protected static final int P9524_AIXFER_POLL = 0x0;
	protected static final int P9524_AIXFER_DMA = 0x1;
	/*Poll All Channels*/
	protected static final int P9524_AIPOLL_ALLCHANNELS = 8;
	protected static final int P9524_AIPOLLSCANS_CH0_CH3 = 8;
	protected static final int P9524_AIPOLLSCANS_CH0_CH2 = 9;
	protected static final int P9524_AIPOLLSCANS_CH0_CH1 = 10;
	/*ADC Sampling Rate*/
	protected static final int P9524_ADC_30K_SPS = 0;
	protected static final int P9524_ADC_15K_SPS = 1;
	protected static final int P9524_ADC_7K5_SPS = 2;
	protected static final int P9524_ADC_3K75_SPS = 3;
	protected static final int P9524_ADC_2K_SPS = 4;
	protected static final int P9524_ADC_1K_SPS = 5;
	protected static final int P9524_ADC_500_SPS = 6;
	protected static final int P9524_ADC_100_SPS = 7;
	protected static final int P9524_ADC_60_SPS = 8;
	protected static final int P9524_ADC_50_SPS = 9;
	protected static final int P9524_ADC_30_SPS = 10;
	protected static final int P9524_ADC_25_SPS = 11;
	protected static final int P9524_ADC_15_SPS = 12;
	protected static final int P9524_ADC_10_SPS = 13;
	protected static final int P9524_ADC_5_SPS = 14;
	protected static final int P9524_ADC_2R5_SPS = 15;
	/*ConfigCtrl Constants*/
	protected static final int P9524_VEX_Range_2R5V = 0x0;
	protected static final int P9524_VEX_Range_10V = 0x1;
	protected static final int P9524_VEX_Sence_Local = 0x0;
	protected static final int P9524_VEX_Sence_Remote = 0x2;
	protected static final int P9524_AIAZMode = 0x4;
	protected static final int P9524_AIBufAutoReset = 0x8;
	protected static final int P9524_AIEnEOCInt = 0x10;
	protected static final int P9524_AIHiSpeed = 0x20;
	/*Trigger Constants*/
	protected static final int P9524_TRGMOD_POST = 0x00;
	protected static final int P9524_TRGSRC_SOFT = 0x00;
	protected static final int P9524_TRGSRC_ExtD = 0x01;
	protected static final int P9524_TRGSRC_SSI = 0x02;
	protected static final int P9524_TRGSRC_QD0 = 0x03;
	protected static final int P9524_TRGSRC_PG0 = 0x04;
	protected static final int P9524_AITrgPositive = 0x00;
	protected static final int P9524_AITrgNegative = 0x08;
	/*Group*/
	protected static final int P9524_AILC_Group = 0;
	protected static final int P9524_AIGP_Group = 1;
	/*Channel*/
	protected static final int P9524_AILC_CH0 = 0;
	protected static final int P9524_AILC_CH1 = 1;
	protected static final int P9524_AILC_CH2 = 2;
	protected static final int P9524_AILC_CH3 = 3;
	protected static final int P9524_AIGP_CH0 = 4;
	protected static final int P9524_AIGP_CH1 = 5;
	protected static final int P9524_AIGP_CH2 = 6;
	protected static final int P9524_AIGP_CH3 = 7;
	/*Pulse Generation and Quadrature Decoder*/
	protected static final int P9524_CTRPG0 = 0;
	protected static final int P9524_CTRPG1 = 1;
	protected static final int P9524_CTRPG2 = 2;
	protected static final int P9524_CTRQD0 = 3;
	protected static final int P9524_CTRQD1 = 4;
	protected static final int P9524_CTRQD2 = 5;
	protected static final int P9524_CTRINTCOUNTER = 6;
	/*Counter Mode*/
	protected static final int P9524_PulseGen_OUTDIR_N = 0;
	protected static final int P9524_PulseGen_OUTDIR_R = 1;
	protected static final int P9524_PulseGen_CW = 0;
	protected static final int P9524_PulseGen_CCW = 2;
	protected static final int P9524_x4_AB_Phase_Decoder = 3;
	protected static final int P9524_Timer = 4;
	/*Counter Op*/
	protected static final int P9524_CTREnable = 0;
	/*Event Mode*/
	protected static final int P9524_Event_Timer = 0;
	/*AO*/
	protected static final int P9524_AOCH0_1 = 0;
	
	/*------------------------*/
	/* Constants for PCI-6202 */
	/*------------------------*/
	/*DIO channel*/
	protected static final int P6202_ISO0 = 0;
	protected static final int P6202_TTL0 = 1;
	/*GPTC/Encoder channel*/
	protected static final int P6202_GPTC0 = 0x00;
	protected static final int P6202_GPTC1 = 0x01;
	protected static final int P6202_ENCODER0 = 0x02;
	protected static final int P6202_ENCODER1 = 0x03;
	protected static final int P6202_ENCODER2 = 0x04;
	/*DA control constant*/
	protected static final int P6202_DA_WRSRC_Int = 0x00;
	protected static final int P6202_DA_WRSRC_AFI0 = 0x01;
	protected static final int P6202_DA_WRSRC_SSI = 0x02;
	protected static final int P6202_DA_WRSRC_AFI1 = 0x03;
	/*DA trigger constant*/
	protected static final int P6202_DA_TRGSRC_SOFT = 0x00;
	protected static final int P6202_DA_TRGSRC_AFI0 = 0x01;
	protected static final int P6202_DA_TRGSRC_SSI = 0x02;
	protected static final int P6202_DA_TRGSRC_AFI1 = 0x03;
	protected static final int P6202_DA_TRGMOD_POST = 0x00;
	protected static final int P6202_DA_TRGMOD_DELAY = 0x04;
	protected static final int P6202_DA_ReTrigEn = 0x20;
	protected static final int P6202_DA_DLY2En = 0x100;
	/*SSI signal code*/
	protected static final int P6202_SSI_DA_CONV = 0x04;
	protected static final int P6202_SSI_DA_TRIG = 0x40;
	/*Encoder constant*/
	protected static final int P6202_EVT_TYPE_EPT0 = 0x00;
	protected static final int P6202_EVT_TYPE_EPT1 = 0x01;
	protected static final int P6202_EVT_TYPE_EPT2 = 0x02;
	protected static final int P6202_EVT_TYPE_EZC0 = 0x03;
	protected static final int P6202_EVT_TYPE_EZC1 = 0x04;
	protected static final int P6202_EVT_TYPE_EZC2 = 0x05;
	
	protected static final int P6202_EVT_MOD_EPT = 0x00;
	
	protected static final int P6202_EPT_PULWIDTH_200us = 0x00;
	protected static final int P6202_EPT_PULWIDTH_2ms = 0x01;
	protected static final int P6202_EPT_PULWIDTH_20ms = 0x02;
	protected static final int P6202_EPT_PULWIDTH_200ms = 0x03;
	
	protected static final int P6202_EPT_TRGOUT_CALLBACK = 0x04;
	protected static final int P6202_EPT_TRGOUT_AFI = 0x08;
	
	protected static final int P6202_ENCODER0_LDATA = 0x05;
	protected static final int P6202_ENCODER1_LDATA = 0x06;
	protected static final int P6202_ENCODER2_LDATA = 0x07;
	
	/*AFI Port*/
	protected static final int P6202_AFI_0 = 0;
	protected static final int P6202_AFI_1 = 1;
	
	/*AFI Mode*/
	protected static final int P6202_AFI_SYNCIntTrigOut = 0;
	
	/*AFI Trigout Length*/
	protected static final int P6202_AFI_SYNCTrig_200ns = 0;
	protected static final int P6202_AFI_SYNCTrig_2ms = 1;
	protected static final int P6202_AFI_SYNCTrig_20ms = 2;
	protected static final int P6202_AFI_SYNCTrig_200ms = 3;
	
	/*------------------------*/
	/* Constants for PCI-922x */
	/*------------------------*/
	/*
	 * AI Constants
	 */
	/*Input Type*/
	protected static final int P922x_AISingEnded = 0x00;
	protected static final int P922x_AINonRef_SingEnded = 0x01;
	protected static final int P922x_AIDifferential = 0x02;
	/*Conversion Source*/
	protected static final int P922x_AICONVSRC_INT = 0x00;
	protected static final int P922x_AICONVSRC_GPI0 = 0x10;
	protected static final int P922x_AICONVSRC_GPI1 = 0x20;
	protected static final int P922x_AICONVSRC_GPI2 = 0x30;
	protected static final int P922x_AICONVSRC_GPI3 = 0x40;
	protected static final int P922x_AICONVSRC_GPI4 = 0x50;
	protected static final int P922x_AICONVSRC_GPI5 = 0x60;
	protected static final int P922x_AICONVSRC_GPI6 = 0x70;
	protected static final int P922x_AICONVSRC_GPI7 = 0x80;
	protected static final int P922x_AICONVSRC_SSI1 = 0x90;
	protected static final int P922x_AICONVSRC_SSI = 0x90;
	/*Trigger Mode*/
	protected static final int P922x_AITRGMOD_POST = 0x00;
	protected static final int P922x_AITRGMOD_GATED = 0x01;
	/*Trigger Source*/
	protected static final int P922x_AITRGSRC_SOFT = 0x00;
	protected static final int P922x_AITRGSRC_GPI0 = 0x10;
	protected static final int P922x_AITRGSRC_GPI1 = 0x20;
	protected static final int P922x_AITRGSRC_GPI2 = 0x30;
	protected static final int P922x_AITRGSRC_GPI3 = 0x40;
	protected static final int P922x_AITRGSRC_GPI4 = 0x50;
	protected static final int P922x_AITRGSRC_GPI5 = 0x60;
	protected static final int P922x_AITRGSRC_GPI6 = 0x70;
	protected static final int P922x_AITRGSRC_GPI7 = 0x80;
	protected static final int P922x_AITRGSRC_SSI5 = 0x90;
	protected static final int P922x_AITRGSRC_SSI = 0x90;
	/*Trigger Polarity*/
	protected static final int P922x_AITrgPositive = 0x000;
	protected static final int P922x_AITrgNegative = 0x100;
	/*ReTrigger*/
	protected static final int P922x_AIEnReTigger = 0x200;
	
	/*
	 * AO Constants
	 */
	/*Conversion Source*/
	protected static final int P922x_AOCONVSRC_INT = 0x00;
	protected static final int P922x_AOCONVSRC_GPI0 = 0x01;
	protected static final int P922x_AOCONVSRC_GPI1 = 0x02;
	protected static final int P922x_AOCONVSRC_GPI2 = 0x03;
	protected static final int P922x_AOCONVSRC_GPI3 = 0x04;
	protected static final int P922x_AOCONVSRC_GPI4 = 0x05;
	protected static final int P922x_AOCONVSRC_GPI5 = 0x06;
	protected static final int P922x_AOCONVSRC_GPI6 = 0x07;
	protected static final int P922x_AOCONVSRC_GPI7 = 0x08;
	protected static final int P922x_AOCONVSRC_SSI2 = 0x09;
	protected static final int P922x_AOCONVSRC_SSI = 0x09;
	/*Trigger Mode*/
	protected static final int P922x_AOTRGMOD_POST = 0x00;
	protected static final int P922x_AOTRGMOD_DELAY = 0x01;
	/*Trigger Source*/
	protected static final int P922x_AOTRGSRC_SOFT = 0x00;
	protected static final int P922x_AOTRGSRC_GPI0 = 0x10;
	protected static final int P922x_AOTRGSRC_GPI1 = 0x20;
	protected static final int P922x_AOTRGSRC_GPI2 = 0x30;
	protected static final int P922x_AOTRGSRC_GPI3 = 0x40;
	protected static final int P922x_AOTRGSRC_GPI4 = 0x50;
	protected static final int P922x_AOTRGSRC_GPI5 = 0x60;
	protected static final int P922x_AOTRGSRC_GPI6 = 0x70;
	protected static final int P922x_AOTRGSRC_GPI7 = 0x80;
	protected static final int P922x_AOTRGSRC_SSI6 = 0x90;
	protected static final int P922x_AOTRGSRC_SSI = 0x90;
	/*Trigger Polarity*/
	protected static final int P922x_AOTrgPositive = 0x000;
	protected static final int P922x_AOTrgNegative = 0x100;
	protected static final int P922x_AOEnReTigger = 0x200;
	protected static final int P922x_AOEnDelay2 = 0x400;
	
	/*
	 * DI Constants
	 */
	/*Conversion Source*/
	protected static final int P922x_DICONVSRC_INT = 0x00;
	protected static final int P922x_DICONVSRC_GPI0 = 0x01;
	protected static final int P922x_DICONVSRC_GPI1 = 0x02;
	protected static final int P922x_DICONVSRC_GPI2 = 0x03;
	protected static final int P922x_DICONVSRC_GPI3 = 0x04;
	protected static final int P922x_DICONVSRC_GPI4 = 0x05;
	protected static final int P922x_DICONVSRC_GPI5 = 0x06;
	protected static final int P922x_DICONVSRC_GPI6 = 0x07;
	protected static final int P922x_DICONVSRC_GPI7 = 0x08;
	protected static final int P922x_DICONVSRC_ADCONV = 0x09;
	protected static final int P922x_DICONVSRC_DACONV = 0x0A;
	/*Trigger Mode*/
	protected static final int P922x_DITRGMOD_POST = 0x00;
	/*Trigger Source*/
	protected static final int P922x_DITRGSRC_SOFT = 0x00;
	protected static final int P922x_DITRGSRC_GPI0 = 0x10;
	protected static final int P922x_DITRGSRC_GPI1 = 0x20;
	protected static final int P922x_DITRGSRC_GPI2 = 0x30;
	protected static final int P922x_DITRGSRC_GPI3 = 0x40;
	protected static final int P922x_DITRGSRC_GPI4 = 0x50;
	protected static final int P922x_DITRGSRC_GPI5 = 0x60;
	protected static final int P922x_DITRGSRC_GPI6 = 0x70;
	protected static final int P922x_DITRGSRC_GPI7 = 0x80;
	/*Trigger Polarity*/
	protected static final int P922x_DITrgPositive = 0x000;
	protected static final int P922x_DITrgNegative = 0x100;
	/*ReTrigger*/
	protected static final int P922x_DIEnReTigger = 0x200;
	
	/*
	 * DO Constants
	 */
	/*Conversion Source*/
	protected static final int P922x_DOCONVSRC_INT = 0x00;
	protected static final int P922x_DOCONVSRC_GPI0 = 0x01;
	protected static final int P922x_DOCONVSRC_GPI1 = 0x02;
	protected static final int P922x_DOCONVSRC_GPI2 = 0x03;
	protected static final int P922x_DOCONVSRC_GPI3 = 0x04;
	protected static final int P922x_DOCONVSRC_GPI4 = 0x05;
	protected static final int P922x_DOCONVSRC_GPI5 = 0x06;
	protected static final int P922x_DOCONVSRC_GPI6 = 0x07;
	protected static final int P922x_DOCONVSRC_GPI7 = 0x08;
	protected static final int P922x_DOCONVSRC_ADCONV = 0x09;
	protected static final int P922x_DOCONVSRC_DACONV = 0x0A;
	/*Trigger Mode*/
	protected static final int P922x_DOTRGMOD_POST = 0x00;
	protected static final int P922x_DOTRGMOD_DELAY = 0x01;
	/*Trigger Source*/
	protected static final int P922x_DOTRGSRC_SOFT = 0x00;
	protected static final int P922x_DOTRGSRC_GPI0 = 0x10;
	protected static final int P922x_DOTRGSRC_GPI1 = 0x20;
	protected static final int P922x_DOTRGSRC_GPI2 = 0x30;
	protected static final int P922x_DOTRGSRC_GPI3 = 0x40;
	protected static final int P922x_DOTRGSRC_GPI4 = 0x50;
	protected static final int P922x_DOTRGSRC_GPI5 = 0x60;
	protected static final int P922x_DOTRGSRC_GPI6 = 0x70;
	protected static final int P922x_DOTRGSRC_GPI7 = 0x80;
	/*Trigger Polarity*/
	protected static final int P922x_DOTrgPositive = 0x000;
	protected static final int P922x_DOTrgNegative = 0x100;
	protected static final int P922x_DOEnReTigger = 0x200;
	
	/*
	 * Encoder/GPTC Constants
	 */
	protected static final int P922x_GPTC0 = 0x00;
	protected static final int P922x_GPTC1 = 0x01;
	protected static final int P922x_GPTC2 = 0x02;
	protected static final int P922x_GPTC3 = 0x03;
	protected static final int P922x_ENCODER0 = 0x04;
	protected static final int P922x_ENCODER1 = 0x05;
	/*Encoder Setting Event Mode*/
	protected static final int P922x_EVT_MOD_EPT = 0x00;
	/*Encoder Setting Event Control*/
	protected static final int P922x_EPT_PULWIDTH_200us = 0x00;
	protected static final int P922x_EPT_PULWIDTH_2ms = 0x01;
	protected static final int P922x_EPT_PULWIDTH_20ms = 0x02;
	protected static final int P922x_EPT_PULWIDTH_200ms = 0x03;
	protected static final int P922x_EPT_TRGOUT_GPO = 0x04;
	protected static final int P922x_EPT_TRGOUT_CALLBACK = 0x08;
	/*Event Type*/
	protected static final int P922x_EVT_TYPE_EPT0 = 0x00;
	protected static final int P922x_EVT_TYPE_EPT1 = 0x01;
	
	/*
	 * SSI signal code
	 */
	protected static final int P922x_SSI_AICONV = 0x02;
	protected static final int P922x_SSI_AITRIG = 0x20;
	protected static final int P922x_SSI_AOCONV = 0x04;
	protected static final int P922x_SSI_AOTRIG = 0x40;
	
	
	/*-------------------------*/
	/* Constants for PCIe-7350 */
	/*-------------------------*/
	protected static final int P7350_PortDIO = 0;
	protected static final int P7350_PortAFI = 1;
	/*DIO Port*/
	protected static final int P7350_DIOA = 0;
	protected static final int P7350_DIOB = 1;
	protected static final int P7350_DIOC = 2;
	protected static final int P7350_DIOD = 3;
	/*AFI Port*/
	protected static final int P7350_AFI_0 = 0;
	protected static final int P7350_AFI_1 = 1;
	protected static final int P7350_AFI_2 = 2;
	protected static final int P7350_AFI_3 = 3;
	protected static final int P7350_AFI_4 = 4;
	protected static final int P7350_AFI_5 = 5;
	protected static final int P7350_AFI_6 = 6;
	protected static final int P7350_AFI_7 = 7;
	/*AFI Mode*/
	protected static final int P7350_AFI_DIStartTrig = 0;
	protected static final int P7350_AFI_DOStartTrig = 1;
	protected static final int P7350_AFI_DIPauseTrig = 2;
	protected static final int P7350_AFI_DOPauseTrig = 3;
	protected static final int P7350_AFI_DISWTrigOut = 4;
	protected static final int P7350_AFI_DOSWTrigOut = 5;
	protected static final int P7350_AFI_COSTrigOut = 6;
	protected static final int P7350_AFI_PMTrigOut = 7;
	protected static final int P7350_AFI_HSDIREQ = 8;
	protected static final int P7350_AFI_HSDIACK = 9;
	protected static final int P7350_AFI_HSDITRIG = 10;
	protected static final int P7350_AFI_HSDOREQ = 11;
	protected static final int P7350_AFI_HSDOACK = 12;
	protected static final int P7350_AFI_HSDOTRIG = 13;
	protected static final int P7350_AFI_SPI = 14;
	protected static final int P7350_AFI_I2C = 15;
	protected static final int P7350_POLL_DI = 16;
	protected static final int P7350_POLL_DO = 17;
	/*Operation Mode*/
	protected static final int P7350_FreeRun = 0;
	protected static final int P7350_HandShake = 1;
	protected static final int P7350_BurstHandShake = 2;
	/*Trigger Status*/
	protected static final int P7350_WAIT_NO = 0;
	protected static final int P7350_WAIT_EXTTRG = 1;
	protected static final int P7350_WAIT_SOFTTRG = 2;
	/*Sampled Clock*/
	protected static final int P7350_IntSampledCLK = 0x00;
	protected static final int P7350_ExtSampledCLK = 0x01;
	/*Sampled Clock Edge*/
	protected static final int P7350_SampledCLK_R = 0x00;
	protected static final int P7350_SampledCLK_F = 0x02;
	/*Enable Export Sample Clock*/
	protected static final int P7350_EnExpSampledCLK = 0x04;
	/*Trigger Configuration*/
	protected static final int P7350_EnPauseTrig = 0x01;
	protected static final int P7350_EnSoftTrigOut = 0x02;
	/*HandShake & Trigger Polarity*/
	protected static final int P7350_DIREQ_POS = 0x00;
	protected static final int P7350_DIREQ_NEG = 0x01;
	protected static final int P7350_DIACK_POS = 0x00;
	protected static final int P7350_DIACK_NEG = 0x02;
	protected static final int P7350_DITRIG_POS = 0x00;
	protected static final int P7350_DITRIG_NEG = 0x04;
	protected static final int P7350_DIStartTrig_POS = 0x00;
	protected static final int P7350_DIStartTrig_NEG = 0x08;
	protected static final int P7350_DIPauseTrig_POS = 0x00;
	protected static final int P7350_DIPauseTrig_NEG = 0x10;
	protected static final int P7350_DOREQ_POS = 0x00;
	protected static final int P7350_DOREQ_NEG = 0x01;
	protected static final int P7350_DOACK_POS = 0x00;
	protected static final int P7350_DOACK_NEG = 0x02;
	protected static final int P7350_DOTRIG_POS = 0x00;
	protected static final int P7350_DOTRIG_NEG = 0x04;
	protected static final int P7350_DOStartTrig_POS = 0x00;
	protected static final int P7350_DOStartTrig_NEG = 0x08;
	protected static final int P7350_DOPauseTrig_POS = 0x00;
	protected static final int P7350_DOPauseTrig_NEG = 0x10;
	/*External Sampled Clock Source*/
	protected static final int P7350_ECLK_IN = 8;
	/*Export Sampled Clock*/
	protected static final int P7350_ECLK_OUT = 8;
	/*Enable Dynamic Delay Adjust*/
	protected static final int P7350_DisDDA = 0x0;
	protected static final int P7350_EnDDA = 0x1;
	/*Dynamic Delay Adjust Mode*/
	protected static final int P7350_DDA_Lag = 0x0;
	protected static final int P7350_DDA_Lead = 0x2;
	/*Dynamic Delay Adjust Step*/
	protected static final int P7350_DDA_130PS = 0;
	protected static final int P7350_DDA_260PS = 1;
	protected static final int P7350_DDA_390PS = 2;
	protected static final int P7350_DDA_520PS = 3;
	protected static final int P7350_DDA_650PS = 4;
	protected static final int P7350_DDA_780PS = 5;
	protected static final int P7350_DDA_910PS = 6;
	protected static final int P7350_DDA_1R04NS = 7;
	/*Enable Dynamic Phase Adjust*/
	protected static final int P7350_DisDPA = 0x0;
	protected static final int P7350_EnDPA = 0x1;
	/*Dynamic Delay Adjust Degree*/
	protected static final int P7350_DPA_0DG = 0;
	protected static final int P7350_DPA_22R5DG = 1;
	protected static final int P7350_DPA_45DG = 2;
	protected static final int P7350_DPA_67R5DG = 3;
	protected static final int P7350_DPA_90DG = 4;
	protected static final int P7350_DPA_112R5DG = 5;
	protected static final int P7350_DPA_135DG = 6;
	protected static final int P7350_DPA_157R5DG = 7;
	protected static final int P7350_DPA_180DG = 8;
	protected static final int P7350_DPA_202R5DG = 9;
	protected static final int P7350_DPA_225DG = 10;
	protected static final int P7350_DPA_247R5DG = 11;
	protected static final int P7350_DPA_270DG = 12;
	protected static final int P7350_DPA_292R5DG = 13;
	protected static final int P7350_DPA_315DG = 14;
	protected static final int P7350_DPA_337R5DG = 15;
	
	/*-------------------------*/
	/* Constants for PCIe-7360 */
	/*-------------------------*/
	protected static final int P7360_PortDIO = 0;
	protected static final int P7360_PortAFI = 1;
	protected static final int P7360_PortECLK = 2;
	/*DIO Port*/
	protected static final int P7360_DIOA = 0;
	protected static final int P7360_DIOB = 1;
	protected static final int P7360_DIOC = 2;
	protected static final int P7360_DIOD = 3;
	/*AFI Port*/
	protected static final int P7360_AFI_0 = 0;
	protected static final int P7360_AFI_1 = 1;
	protected static final int P7360_AFI_2 = 2;
	protected static final int P7360_AFI_3 = 3;
	protected static final int P7360_AFI_4 = 4;
	protected static final int P7360_AFI_5 = 5;
	protected static final int P7360_AFI_6 = 6;
	protected static final int P7360_AFI_7 = 7;
	/*AFI Mode*/
	protected static final int P7360_AFI_DIStartTrig = 0;
	protected static final int P7360_AFI_DOStartTrig = 1;
	protected static final int P7360_AFI_DIPauseTrig = 2;
	protected static final int P7360_AFI_DOPauseTrig = 3;
	protected static final int P7360_AFI_DISWTrigOut = 4;
	protected static final int P7360_AFI_DOSWTrigOut = 5;
	protected static final int P7360_AFI_COSTrigOut = 6;
	protected static final int P7360_AFI_PMTrigOut = 7;
	protected static final int P7360_AFI_HSDIREQ = 8;
	protected static final int P7360_AFI_HSDIACK = 9;
	protected static final int P7360_AFI_HSDITRIG = 10;
	protected static final int P7360_AFI_HSDOREQ = 11;
	protected static final int P7360_AFI_HSDOACK = 12;
	protected static final int P7360_AFI_HSDOTRIG = 13;
	protected static final int P7360_AFI_SPI = 14;
	protected static final int P7360_AFI_I2C = 15;
	protected static final int P7360_POLL_DI = 16;
	protected static final int P7360_POLL_DO = 17;
	/*Operation Mode*/
	protected static final int P7360_FreeRun = 0;
	protected static final int P7360_HandShake = 1;
	protected static final int P7360_BurstHandShake = 2;
	protected static final int P7360_BurstHandShake2 = 3;
	/*Trigger Status*/
	protected static final int P7360_WAIT_NO = 0;
	protected static final int P7360_WAIT_EXTTRG = 1;
	protected static final int P7360_WAIT_SOFTTRG = 2;
	protected static final int P7360_WAIT_PATMATCH = 3;
	/*Sampled Clock*/
	protected static final int P7360_IntSampledCLK = 0x00;
	protected static final int P7360_ExtSampledCLK = 0x01;
	/*Sampled Clock Edge*/
	protected static final int P7360_SampledCLK_R = 0x00;
	protected static final int P7360_SampledCLK_F = 0x02;
	/*Enable Export Sample Clock*/
	protected static final int P7360_EnExpSampledCLK = 0x04;
	/*Trigger Configuration*/
	protected static final int P7360_EnPauseTrig = 0x01;
	protected static final int P7360_EnSoftTrigOut = 0x02;
	/*HandShake & Trigger Polarity*/
	protected static final int P7360_DIREQ_POS = 0x00;
	protected static final int P7360_DIREQ_NEG = 0x01;
	protected static final int P7360_DIACK_POS = 0x00;
	protected static final int P7360_DIACK_NEG = 0x02;
	protected static final int P7360_DITRIG_POS = 0x00;
	protected static final int P7360_DITRIG_NEG = 0x04;
	protected static final int P7360_DIStartTrig_POS = 0x00;
	protected static final int P7360_DIStartTrig_NEG = 0x08;
	protected static final int P7360_DIPauseTrig_POS = 0x00;
	protected static final int P7360_DIPauseTrig_NEG = 0x10;
	protected static final int P7360_DOREQ_POS = 0x00;
	protected static final int P7360_DOREQ_NEG = 0x01;
	protected static final int P7360_DOACK_POS = 0x00;
	protected static final int P7360_DOACK_NEG = 0x02;
	protected static final int P7360_DOTRIG_POS = 0x00;
	protected static final int P7360_DOTRIG_NEG = 0x04;
	protected static final int P7360_DOStartTrig_POS = 0x00;
	protected static final int P7360_DOStartTrig_NEG = 0x08;
	protected static final int P7360_DOPauseTrig_POS = 0x00;
	protected static final int P7360_DOPauseTrig_NEG = 0x10;
	/*External Sampled Clock Source*/
	protected static final int P7360_ECLK_IN = 8;
	/*Export Sampled Clock*/
	protected static final int P7360_ECLK_OUT = 8;
	/*Enable Dynamic Delay Adjust*/
	protected static final int P7360_DisDDA = 0x0;
	protected static final int P7360_EnDDA = 0x1;
	/*Dynamic Delay Adjust Mode*/
	protected static final int P7360_DDA_Lag = 0x0;
	protected static final int P7360_DDA_Lead = 0x2;
	/*Enable Dynamic Phase Adjust*/
	protected static final int P7360_DisDPA = 0x0;
	protected static final int P7360_EnDPA = 0x1;
	/*ECLK output type control*/
	protected static final int P7360_ECLKAligned = 0x0;
	protected static final int P7360_ECLKAlwaysOn = 0x1;
	
	/*---------------------------------*/
	/* Constants for I Squared C (I2C) */
	/*---------------------------------*/
	/*I2C Port*/
	protected static final int I2C_Port_A = 0;
	/*I2C Control Operation*/
	protected static final int I2C_ENABLE = 0;
	protected static final int I2C_STOP = 1;
	
	
	/*-------------------------------------------*/
	/* Constants for Serial Peripheral Interface */
	/*-------------------------------------------*/
	/*SPI Port*/
	protected static final int SPI_Port_A = 0;
	/*SPI Clock Mode*/
	protected static final int SPI_CLK_L = 0x00;
	protected static final int SPI_CLK_H = 0x01;
	/*SPI TX Polarity*/
	protected static final int SPI_TX_POS = 0x00;
	protected static final int SPI_TX_NEG = 0x02;
	/*SPI RX Polarity*/
	protected static final int SPI_RX_POS = 0x00;
	protected static final int SPI_RX_NEG = 0x04;
	/*SPI Transferred Order*/
	protected static final int SPI_MSB = 0x00;
	protected static final int SPI_LSB = 0x08;
	/*SPI Control Operation*/
	protected static final int SPI_ENABLE = 0;
	
	
	/*-----------------------------*/
	/* Constants for Pattern Match */
	/*-----------------------------*/
	/*Pattern Match Channel Mode*/
	protected static final int PATMATCH_CHNDisable = 0;
	protected static final int PATMATCH_CHNEnable = 1;
	/*Pattern Match Channel Type*/
	protected static final int PATMATCH_Level_L = 0;
	protected static final int PATMATCH_Level_H = 1;
	protected static final int PATMATCH_Edge_R = 2;
	protected static final int PATMATCH_Edge_F = 3;
	/*Pattern Match Operation*/
	protected static final int PATMATCH_STOP = 0;
	protected static final int PATMATCH_START = 1;
	protected static final int PATMATCH_RESTART = 2;
	
	/*---------------------------------------------*/
	/* Constants for Access EEPROM  			   */
	/*---------------------------------------------*/
	/*for PCI-7230/PCMe-7230*/
	protected static final int P7230_EEP_BLK_0 = 0;
	protected static final int P7230_EEP_BLK_1 = 1;
	/*for PCI-9221/PCI-9222*/
	protected static final int P9221_EEP_BLK_0 = 0;
	protected static final int P922x_EEP_BLK_0 = 0;
	
	/*----------------------------------------------------------------------------*/
	/* PCIS-DASK Function prototype                                               */
	/*----------------------------------------------------------------------------*/
	/*----------------------------------------------------------------------------*/
	/* Basic Function */
	protected static native int  RegisterCard(int CardType, int card_num);
	
	protected static native int  ReleaseCard(int CardNumber);
	
	/*----------------------------------------------------------------------------*/
	/* AI Function */
	protected static native int  AI9111Config(int CardNumber, int TrigSource, int TrgMode,  int TraceCnt);
	
	protected static native int  AI9112Config(int CardNumber, int TrigSource);
	
	protected static native int  AI9113Config(int CardNumber, int TrigSource);
	
	protected static native int  AI9114Config(int CardNumber, int TrigSource);
	
	protected static native int  AI9114PreTrigConfig(int CardNumber, int PreTrgEn, int TraceCnt);
	
	protected static native int  AI9116Config(int CardNumber, int ConfigCtrl, int TrigCtrl,  int PostCnt, int MCnt, int ReTrgCnt);
	
	protected static native int  AI9116CounterInterval(int CardNumber, int ScanIntrv,  int SampIntrv);
	
	protected static native int  AI9118Config(int CardNumber, int ModeCtrl, int FunCtrl,  int BurstCnt, int PostCnt);
	
	protected static native int  AI9221Config(int CardNumber, int ConfigCtrl, int TrigCtrl,  boolean AutoResetBuf);
	
	protected static native int  AI9221CounterInterval(int CardNumber, int ScanIntrv,  int SampIntrv);
	
	protected static native int  AI9812Config(int CardNumber, int TrgMode, int TrgSrc,  int TrgPol, int ClkSel, int TrgLevel, int PostCnt);
	
	protected static native int  AI9812SetDiv(int CardNumber, int PacerVal);
	
	protected static native int  AI9524Config(int CardNumber, int Group, int XMode,  int ConfigCtrl, int TrigCtrl, int TrigValue);
	
	protected static native int  AI9524PollConfig(int CardNumber, int Group, int PollChannel,  int PollRange, int PollSpeed);
	
	protected static native int  AI9524SetDSP(int CardNumber, int Channel, int Mode,  int DFStage, int SPKRejThreshold);
	
	protected static native int  AI9524GetEOCEvent(int CardNumber, int Group, long[]hEvent);
	
	protected static native int  AI9222Config(int CardNumber, int ConfigCtrl, int TrigCtrl,  int ReTriggerCnt, boolean AutoResetBuf);
	
	protected static native int  AI9222CounterInterval(int CardNumber, int ScanIntrv,  int SampIntrv);
	
	protected static native int  AI9223Config(int CardNumber, int ConfigCtrl, int TrigCtrl,  int ReTriggerCnt, boolean AutoResetBuf);
	
	protected static native int  AI9223CounterInterval(int CardNumber, int ScanIntrv,  int SampIntrv);
	
	protected static native int  AI922AConfig(int CardNumber, int ConfigCtrl, int TrigCtrl,  int ReTriggerCnt, boolean AutoResetBuf);
	
	protected static native int  AI922ACounterInterval(int CardNumber, int ScanIntrv,  int SampIntrv);
	
	protected static native int  AIAsyncCheck(int CardNumber, boolean[] Stopped, int[] AccessCnt);
	
	protected static native int  AIAsyncClear(int CardNumber, int[] AccessCnt);
	
	protected static native int  AIAsyncDblBufferHalfReady(int CardNumber, boolean[] HalfReady,  boolean[] StopFlag);
	
	protected static native int  AIAsyncDblBufferMode(int CardNumber, boolean Enable);
	
	protected static native int  AIAsyncDblBufferTransfer(int CardNumber, int[] Buffer);
	
	protected static native int  AIAsyncDblBufferOverrun(int CardNumber, int op,  int[] overrunFlag);
	
	protected static native int  AIAsyncDblBufferHandled(int CardNumber);
	
	protected static native int  AIAsyncDblBufferToFile(int CardNumber);
	
	protected static native int  AIAsyncReTrigNextReady(int CardNumber, boolean[] Ready,  boolean[] StopFlag, int[] RdyTrigCnt);
	
	protected static native int  AIContReadChannel(int CardNumber, int Channel, int AdRange,  short[] Buffer, int ReadCount, double SampleRate, int SyncMode);
	
	protected static native int  AIContReadMultiChannels(int CardNumber, int NumChans,  int[] Chans, int[] AdRanges, int[] Buffer, int ReadCount, double SampleRate,  int SyncMode);
	
	protected static native int  AIContScanChannels(int CardNumber, int Channel, int AdRange,  int[] Buffer, int ReadCount, double SampleRate, int SyncMode);
	
	protected static native int  AIContReadChannelToFile(int CardNumber, int Channel,  int AdRange, byte[] FileName, int ReadCount, double SampleRate, int SyncMode);
	
	protected static native int  AIContReadMultiChannelsToFile(int CardNumber, int NumChans,  int[] Chans, int[] AdRanges, byte[] FileName, int ReadCount, double SampleRate,  int SyncMode);
	
	protected static native int  AIContScanChannelsToFile(int CardNumber, int Channel,  int AdRange, byte[] FileName, int ReadCount, double SampleRate, int SyncMode);
	
	protected static native int  AIContStatus(int CardNumber, int[] Status);
	
	protected static native int  AIContBufferSetup(int CardNumber, byte[] Buffer, int ReadCount,  int[] BufferId);
	
	protected static native int  AIContBufferReset(int CardNumber);
	
	protected static native int  AIEventCallBack(int CardNumber, int mode, int EventType,  int callbackAddr);
	
	protected static native int  AIInitialMemoryAllocated(int CardNumber, int[] MemSize);
	
	protected static native int  AIReadChannel(int CardNumber, int Channel, int AdRange,  int[] Value);
	
	protected static native int  AIReadChannel32(int CardNumber, int Channel, int AdRange,  int[] Value);
	
	protected static native int  AIVReadChannel(int CardNumber, int Channel, int AdRange,  double[] voltage);
	
	protected static native int  AIScanReadChannels(int CardNumber, int Channel, int AdRange,  int[] Buffer);
	
	protected static native int  AIScanReadChannels32(int CardNumber, int Channel, int AdRange,  int[] Buffer);
	
	protected static native int  AIReadMultiChannels(int CardNumber, int NumChans, int[] Chans,  int[] AdRanges, int[] Buffer);
	
	protected static native int  AIVoltScale(int CardNumber, int AdRange, int reading,  double[] voltage);
	
	protected static native int  AIVoltScale32(int CardNumber, int adRange, int reading,  double[] voltage);
	
	protected static native int  AIContVScale(int CardNumber, int adRange, byte[] readingArray,  double[] voltageArray, int count);
	
	protected static native int  AISetTimeOut(int CardNumber, int TimeOut);
	
	/*----------------------------------------------------------------------------*/
	/* AO Function */
	protected static native int  AO6202Config(int CardNumber, int ConfigCtrl, int TrigCtrl,  int ReTrgCnt, int DLY1Cnt, int DLY2Cnt, boolean AutoResetBuf);
	
	protected static native int  AO6208AConfig(int CardNumber, int V2AMode);
	
	protected static native int  AO6308AConfig(int CardNumber, int V2AMode);
	
	protected static native int  AO6308VConfig(int CardNumber, int Channel, int OutputPolarity,  double refVoltage);
	
	protected static native int  AO9111Config(int CardNumber, int OutputPolarity);
	
	protected static native int  AO9112Config(int CardNumber, int Channel, double refVoltage);
	
	protected static native int  AO9222Config(int CardNumber, int ConfigCtrl, int TrigCtrl,  int ReTrgCnt, int DLY1Cnt, int DLY2Cnt, boolean AutoResetBuf);
	
	protected static native int  AO9223Config(int CardNumber, int ConfigCtrl, int TrigCtrl,  int ReTrgCnt, int DLY1Cnt, int DLY2Cnt, boolean AutoResetBuf);
	
	protected static native int  AOAsyncCheck(int CardNumber, boolean[] Stopped, int[] AccessCnt);
	
	protected static native int  AOAsyncClear(int CardNumber, int[] AccessCnt, int stop_mode);
	
	protected static native int  AOAsyncDblBufferHalfReady(int CardNumber, boolean[] bHalfReady);
	
	protected static native int  AOAsyncDblBufferMode(int CardNumber, boolean Enable);
	
	protected static native int  AOContBufferCompose(int CardNumber, int TotalChnCount,  int ChnNum, int UpdateCount, byte[] ConBuffer, byte[] Buffer);
	
	protected static native int  AOContBufferReset(int CardNumber);
	
	protected static native int  AOContBufferSetup(int CardNumber, byte[] Buffer, int WriteCount,  int[] BufferId);
	
	protected static native int  AOContStatus(int CardNumber, int[] Status);
	
	protected static native int  AOContWriteChannel(int CardNumber, int Channel, int BufId,  int WriteCount, int Iterations, int CHUI, int definite, int SyncMode);
	
	protected static native int  AOContWriteMultiChannels(int CardNumber, int NumChans,  int[] Chans, int BufId, int WriteCount, int Iterations, int CHUI,  int definite, int SyncMode);
	
	protected static native int  AOEventCallBack(int CardNumber, int mode, int EventType,  int callbackAddr);
	
	protected static native int  AOInitialMemoryAllocated(int CardNumber, int[] MemSize);
	
	protected static native int  AOSetTimeOut(int CardNumber, int TimeOut);
	
	protected static native int  AOSimuVWriteChannel(int CardNumber, int Group, double[] VBuffer);
	
	protected static native int  AOSimuWriteChannel(int CardNumber, int Group, int[] Buffer);
	
	protected static native int  AOVoltScale(int CardNumber, int Channel, double Voltage,  int[] binValue);
	
	protected static native int  AOVWriteChannel(int CardNumber, int Channel, double Voltage);
	
	protected static native int  AOWriteChannel(int CardNumber, int Channel, int Value);
	
	/*----------------------------------------------------------------------------*/
	/* DI Function */
	protected static native int  DI7200Config(int CardNumber, int TrigSource, int ExtTrigEn,  int TrigPol, int I_REQ_Pol);
	
	protected static native int  DI7233_ForceLogic(int CardNumber, int ConfigCtrl);
	
	protected static native int  DI7300AConfig(int CardNumber, int PortWidth, int TrigSource,  int WaitStatus, int Terminator, int I_REQ_Pol, boolean clear_fifo,  boolean disable_di);
	
	protected static native int  DI7300BConfig(int CardNumber, int PortWidth, int TrigSource,  int WaitStatus, int Terminator, int I_Cntrl_Pol, boolean clear_fifo,  boolean disable_di);
	
	protected static native int  DI7350Config(int CardNumber, int DIPortWidth, int DIMode,  int DIWaitStatus, int DIClkConfig);
	
	protected static native int  DI7350ExportSampCLKConfig(int CardNumber, int CLK_Src,  int CLK_DPAMode, int CLK_DPAVlaue);
	
	protected static native int  DI7350ExtSampCLKConfig(int CardNumber, int CLK_Src,  int CLK_DDAMode, int CLK_DPAMode, int CLK_DDAVlaue, int CLK_DPAVlaue);
	
	protected static native int  DI7350SoftTriggerGen(int CardNumber);
	
	protected static native int  DI7350TrigHSConfig(int CardNumber, int TrigConfig, int DIIPOL,  int DIREQSrc, int DIACKSrc, int DITRIGSrc, int StartTrigSrc,  int PauseTrigSrc, int SoftTrigOutSrc, int SoftTrigOutLength, int TrigCount);
	
	protected static native int  DI7350BurstHandShakeDelay(int CardNumber, byte Delay);
	
	protected static native int  DI7360Config(int CardNumber, int DIPortWidth, int DIMode,  int DIWaitStatus, int DIClkConfig);
	
	protected static native int  DI7360ExportSampCLKConfig(int CardNumber, int CLK_Src,  int CLK_DPAMode, int CLK_DPAVlaue);
	
	protected static native int  DI7360ExtSampCLKConfig(int CardNumber, int CLK_Src,  int CLK_DDAMode, int CLK_DPAMode, int CLK_DDAVlaue, int CLK_DPAVlaue);
	
	protected static native int  DI7360SoftTriggerGen(int CardNumber);
	
	protected static native int  DI7360TrigHSConfig(int CardNumber, int TrigConfig, int DIIPOL,  int DIREQSrc, int DIACKSrc, int DITRIGSrc, int StartTrigSrc,  int PauseTrigSrc, int SoftTrigOutSrc, int SoftTrigOutLength, int TrigCount);
	
	protected static native int  DI7360BurstHandShakeDelay(int CardNumber, byte Delay);
	
	protected static native int  DI7360HighSpeedMode(int CardNumber, int wEnable);
	
	protected static native int  DI7360SetDelayStep(int CardNumber, int Step);
	
	protected static native int  DI9222Config(int CardNumber, int ConfigCtrl, int TrigCtrl,  int ReTriggerCnt, boolean AutoResetBuf);
	
	protected static native int  DI9223Config(int CardNumber, int ConfigCtrl, int TrigCtrl,  int ReTriggerCnt, boolean AutoResetBuf);
	
	protected static native int  DIAsyncCheck(int CardNumber, boolean[] Stopped, int[] AccessCnt);
	
	protected static native int  DIAsyncClear(int CardNumber, int[] AccessCnt);
	
	protected static native int  DIAsyncDblBufferHalfReady(int CardNumber, boolean[] HalfReady);
	
	protected static native int  DIAsyncDblBufferHandled(int CardNumber);
	
	protected static native int  DIAsyncDblBufferMode(int CardNumber, boolean Enable);
	
	protected static native int  DIAsyncDblBufferToFile(int CardNumber);
	
	protected static native int  DIAsyncDblBufferTransfer(int CardNumber, byte[] Buffer);
	
	protected static native int  DIAsyncDblBufferOverrun(int CardNumber, int op,  int[] overrunFlag);
	
	protected static native int  DIAsyncMultiBuffersHandled(int CardNumber, int bufcnt,  int[]  bufs);
	
	protected static native int  DIAsyncMultiBufferNextReady(int CardNumber, boolean[] NextReady,  int[] BufferId);
	
	protected static native int  DIAsyncReTrigNextReady(int CardNumber, boolean[] Ready,  boolean[] StopFlag, int[] RdyTrigCnt);
	
	protected static native int  DIContBufferReset(int CardNumber);
	
	protected static native int  DIContBufferSetup(int CardNumber, byte[] Buffer, int ReadCount,  int[] BufferId);
	
	protected static native int  DIContMultiBufferSetup(int CardNumber, byte[] Buffer,  int ReadCount, int[] BufferId);
	
	protected static native int  DIContMultiBufferStart(int CardNumber, int Port, double SampleRate);
	
	protected static native int  DIContReadPort(int CardNumber, int Port, byte[] Buffer,  int ReadCount, double SampleRate, int SyncMode);
	
	protected static native int  DIContReadPortToFile(int CardNumber, int Port, byte[] FileName,  int ReadCount, double SampleRate, int SyncMode);
	
	protected static native int  DIContStatus(int CardNumber, int[] Status);
	
	protected static native int  DIEventCallBack(int CardNumber, int mode, int EventType,  int callbackAddr);
	
	protected static native int  DIInitialMemoryAllocated(int CardNumber, int[] DmaSize);
	
	protected static native int  DIReadLine(int CardNumber, int Port, int Line, int[] State);
	
	protected static native int  DIReadPort(int CardNumber, int Port, int[] Value);
	
	protected static native int  DISetTimeOut(int CardNumber, int TimeOut);
	
	/*----------------------------------------------------------------------------*/
	/* DO Function */
	protected static native int  DO7200Config(int CardNumber, int TrigSource, int OutReqEn,  int OutTrigSig);
	
	protected static native int  DO7300AConfig(int CardNumber, int PortWidth, int TrigSource,  int WaitStatus, int Terminator, int O_REQ_Pol);
	
	protected static native int  DO7300BConfig(int CardNumber, int PortWidth, int TrigSource,  int WaitStatus, int Terminator, int O_Cntrl_Pol, int FifoThreshold);
	
	protected static native int  DO7300B_SetDODisableMode(int CardNumber, int Mode);
	
	protected static native int  DO7350Config(int CardNumber, int DOPortWidth, int DOMode,  int DOWaitStatus, int DOClkConfig);
	
	protected static native int  DO7350ExportSampCLKConfig(int CardNumber, int CLK_Src,  int CLK_DPAMode, int CLK_DPAVlaue);
	
	protected static native int  DO7350ExtSampCLKConfig(int CardNumber, int CLK_Src,  int CLK_DDAMode, int CLK_DPAMode, int CLK_DDAVlaue, int CLK_DPAVlaue);
	
	protected static native int  DO7350SoftTriggerGen(int CardNumber);
	
	protected static native int  DO7350TrigHSConfig(int CardNumber, int TrigConfig, int DOIPOL,  int DOREQSrc, int DOACKSrc, int DOTRIGSrc, int StartTrigSrc,  int PauseTrigSrc, int SoftTrigOutSrc, int SoftTrigOutLength, int TrigCount);
	
	protected static native int  DO7350BurstHandShakeDelay(int CardNumber, byte Delay);
	
	protected static native int  DO7360Config(int CardNumber, int DOPortWidth, int DOMode,  int DOWaitStatus, int DOClkConfig);
	
	protected static native int  DO7360ExportSampCLKConfig(int CardNumber, int CLK_Src,  int CLK_Mode, int CLK_DPAMode, int CLK_DPAVlaue);
	
	protected static native int  DO7360ExtSampCLKConfig(int CardNumber, int CLK_Src,  int CLK_DDAMode, int CLK_DPAMode, int CLK_DDAVlaue, int CLK_DPAVlaue);
	
	protected static native int  DO7360SoftTriggerGen(int CardNumber);
	
	protected static native int  DO7360TrigHSConfig(int CardNumber, int TrigConfig, int DOIPOL,  int DOREQSrc, int DOACKSrc, int DOTRIGSrc, int StartTrigSrc,  int PauseTrigSrc, int SoftTrigOutSrc, int SoftTrigOutLength, int TrigCount);
	
	protected static native int  DO7360BurstHandShakeDelay(int CardNumber, byte Delay);
	
	protected static native int  EDO9111Config(int CardNumber, int EDOFun);
	
	protected static native int  DO9222Config(int CardNumber, int ConfigCtrl, int TrigCtrl,  int ReTrgCnt, int DLY1Cnt, int DLY2Cnt, boolean AutoResetBuf);
	
	protected static native int  DO9223Config(int CardNumber, int ConfigCtrl, int TrigCtrl,  int ReTrgCnt, int DLY1Cnt, int DLY2Cnt, boolean AutoResetBuf);
	
	protected static native int  DOAsyncCheck(int CardNumber, boolean[] Stopped, int[] AccessCnt);
	
	protected static native int  DOAsyncClear(int CardNumber, int[] AccessCnt);
	
	protected static native int  DOAsyncMultiBufferNextReady(int CardNumber, boolean[] NextReady,  int[] BufferId);
	
	protected static native int  DOContBufferReset(int CardNumber);
	
	protected static native int  DOContBufferSetup(int CardNumber, byte[] Buffer, int WriteCount, int[] BufferId);
	
	protected static native int  DOContMultiBufferSetup(int CardNumber, byte[] Buffer,  int WriteCount, int[] BufferId);
	
	protected static native int  DOContMultiBufferStart(int CardNumber, int Port,  double fSampleRate);
	
	protected static native int  DOContStatus(int CardNumber, int[] Status);
	
	protected static native int  DOContWritePort(int CardNumber, int Port, byte[] Buffer,  int WriteCount, int Iterations, double SampleRate, int SyncMode);
	
	protected static native int  DOContWritePortEx(int CardNumber, int Port, byte[] Buffer,  int WriteCount, int Iterations, double SampleRate, int SyncMode);
	
	protected static native int  DOEventCallBack(int CardNumber, int mode, int EventType,  int callbackAddr);
	
	protected static native int  DOInitialMemoryAllocated(int CardNumber, int[] MemSize);
	
	protected static native int  DOPGStart(int CardNumber, byte[] Buffer, int WriteCount,  double SampleRate);
	
	protected static native int  DOPGStop(int CardNumber);
	
	protected static native int  DOReadLine(int CardNumber, int Port, int Line, int[] Value);
	
	protected static native int  DOReadPort(int CardNumber, int Port, int[] Value);
	
	protected static native int  DOResetOutput(int CardNumber, int reset);
	
	protected static native int  DOSetTimeOut(int CardNumber, int TimeOut);
	
	protected static native int  DOSimuWritePort(int CardNumber, int NumChans, int[] Buffer);
	
	protected static native int  DOWriteExtTrigLine(int CardNumber, int Value);
	
	protected static native int  DOWriteLine(int CardNumber, int Port, int Line, int Value);
	
	protected static native int  DOWritePort(int CardNumber, int Port, int Value);
	
	/*----------------------------------------------------------------------------*/
	/* DIO Function */
	protected static native int  DIO7300SetInterrupt(int CardNumber, int AuxDIEn, int T2En,  long[]Event);
	
	protected static native int  DIO7350_AFIConfig(int CardNumber, int AFI_Port, int AFI_Enable,  int AFI_Mode, int AFI_TrigOutLen);
	
	protected static native int  DIO7360_AFIConfig(int CardNumber, int AFI_Port, int AFI_Enable,  int AFI_Mode, int AFI_TrigOutLen);
	
	//protected static native int  DIOAUXDIEventMessage(int CardNumber, int AuxDIEn,  long windowHandle, int message, byte[] callbackAddr());
	
	protected static native int  DIOCOSInterruptCounter(int CardNumber, int Counter_Num,  int Counter_Mode, int DIPort, int DILine, int[] Counter_Value);
	
	protected static native int  DIOGetCOSLatchData(int CardNumber, int[] CosLData);
	
	protected static native int  DIOGetCOSLatchData32(int CardNumber, byte Port, int[] CosLData);
	
	protected static native int  DIOGetCOSLatchDataInt32(int CardNumber, byte Port, int[] CosLData);
	
	protected static native int  DIOGetPMLatchData32(int CardNumber, int Port, int[] PMLData);
	
	//protected static native int  DIOINT1_EventMessage(int CardNumber, int Int1Mode,  long windowHandle, int message, byte[] callbackAddr());
	
	//protected static native int  DIOINT2_EventMessage(int CardNumber, int Int2Mode,  long windowHandle, int message, byte[] callbackAddr());
	
	protected static native int  DIOINT_Event_Message(int CardNumber, int mode, long evt,  long windowHandle, int message, int callbackAddr);
	
	protected static native int  DIOLineConfig(int CardNumber, int Port, int Line, int Direction);
	
	protected static native int  DIOLinesConfig(int CardNumber, int Port, int Linesdirmap);
	
	protected static native int  DIOPortConfig(int CardNumber, int Port, int Direction);
	
	protected static native int  DIOPMConfig(int CardNumber, int Channel, int PM_ChnEn,  int PM_ChnType);
	
	protected static native int  DIOPMControl(int CardNumber, int Port, int PM_Start,  long[]hEvent, boolean ManualReset);
	
	protected static native int  DIOSetCOSInterrupt(int CardNumber, int Port, int ctlA, int ctlB, int ctlC);
	
	protected static native int  DIOSetCOSInterrupt32(int CardNumber, byte Port, int ctl,  long[]Event, boolean ManualReset);
	
	protected static native int  DIOSetPMInterrupt32(int CardNumber, int Port, int Ctrl,  int Pattern1, int Pattern2, long[]hEvent, boolean ManualReset);
	
	protected static native int  DIOSetDualInterrupt(int CardNumber, int Int1Mode, int Int2Mode,  long[]Event);
	
	//protected static native int  DIOT2_EventMessage(int CardNumber, int T2En,  long windowHandle, int message, byte[] callbackAddr());
	
	protected static native int  DIOVoltLevelConfig(int CardNumber, int PortType, int VoltLevel);
	
	protected static native int  DIOAFIConfig(int wCardNumber, int wAFI_Port,  	int wAFI_Enable, int wAFI_Mode, int dwAFI_TrigOutLen);
	/*----------------------------------------------------------------------------*/
	/* Timer/Counter Function */
	protected static native int  CTR8554CK1Config(int CardNumber, int ClockSource);
	
	protected static native int  CTR8554ClkSrcConfig(int CardNumber, int Ctr, int ClockSource);
	
	protected static native int  CTR8554DebounceConfig(int CardNumber, int DebounceClock);
	
	protected static native int  CTRClear(int CardNumber, int Ctr, int State);
	
	protected static native int  CTRRead(int CardNumber, int Ctr, int[] Value);
	
	protected static native int  CTRReadAll(int CardNumber, int CtrCnt, int[] Ctr, int[] Value);
	
	protected static native int  CTRSetup(int CardNumber, int Ctr, int Mode, int Count,  int BinBcd);
	
	protected static native int  CTRSetupAll(int CardNumber, int CtrCnt, int[] Ctr, int[] Mode,  int[] Count, int[] BinBcd);
	
	protected static native int  CTRStatus(int CardNumber, int Ctr, int[] Value);
	
	protected static native int  CTRUpdate(int CardNumber, int Ctr, int Count);
	
	protected static native int  GCTRClear(int CardNumber, int GCtr);
	
	protected static native int  GCTRRead(int CardNumber, int GCtr, int[] Value);
	
	protected static native int  GCTRSetup(int CardNumber, int GCtr, int GCtrCtrl, int Count);
	
	protected static native int  GPTC9524PGConfig(int CardNumber, int GCtr, int PulseGenNum);
	
	protected static native int  GPTCClear(int CardNumber, int GCtr);
	
	protected static native int  GPTCControl(int CardNumber, int GCtr, int ParamID, int Value);
	
	protected static native int  GPTCEventCallBack(int CardNumber, int Enabled, int EventType,  int callbackAddr);
	
	protected static native int  GPTCEventSetup(int CardNumber, int GCtr, int Mode, int Ctrl,  int LVal_1, int LVal_2);
	
	protected static native int  GPTCRead(int CardNumber, int GCtr, int[] Value);
	
	protected static native int  GPTCSetup(int CardNumber, int GCtr, int Mode, int SrcCtrl,  int PolCtrl, int LReg1_Val, int LReg2_Val);
	
	protected static native int  GPTCStatus(int CardNumber, int GCtr, int[] Value);
	
	protected static native int  GPTC9524GetTimerEvent(int CardNumber, int GCtr, long[]hEvent);
	
	protected static native int  GPTC9524SetCombineEcdData(int CardNumber, boolean enable);
	
	protected static native int  WDTControl(int CardNumber, int Ctr, int action);
	
	protected static native int  WDTReload(int CardNumber, int Ctr, float ovflowSec,  float[] actualSec);
	
	protected static native int  WDTSetup(int CardNumber, int Ctr, float ovflowSec,  float[] actualSec, long[]Event);
	
	protected static native int  WDTStatus(int CardNumber, int Ctr, int[] Value);
	
	/*----------------------------------------------------------------------------*/
	/* Get Event or View Function */
	protected static native int  AIGetEvent(int CardNumber, long[]Event);
	
	protected static native int  AOGetEvent(int CardNumber, long[]Event);
	
	protected static native int  DIGetEvent(int CardNumber, long[]Event);
	
	protected static native int  DOGetEvent(int CardNumber, long[]Event);
	
	protected static native int  AIGetView(int CardNumber, int[] View);
	
	protected static native int  DIGetView(int CardNumber, int[] View);
	
	protected static native int  DOGetView(int CardNumber, int[] View);
	
	/*---------------------------------------------------------------------------*/
	/* Common Function */
	protected static native int  GetActualRate(int CardNumber, double SampleRate, double[] ActualRate);
	
	protected static native int  GetActualRate_9524(int CardNumber, int Group, double SampleRate,  double[] ActualRate);
	
	protected static native int  GetBaseAddr(int CardNumber, int[] BaseAddr, int[] BaseAddr2);
	
	protected static native int  GetCardIndexFromID(int CardNumber, int[] cardType, int[] cardIndex);
	
	protected static native int  GetCardType(int CardNumber, int[] cardType);
	
	protected static native int  GetLCRAddr(int CardNumber, int[] LcrAddr);
	
	protected static native int  PCIEEPROMLoadData(int CardNumber, int block, int[] data);
	
	protected static native int  PCIEEPROMSaveData(int CardNumber, int block, int data);
	
	protected static native byte[] PCIBufferAlloc (int CardNumber, long BufferSize);
	
	protected static native int  PCIBufferFree (int CardNumber, byte[] BufferAddr);
	
	protected static native int  PCIGetSerialNumber(int wCardNumber, byte[] SerialString, byte numberOfElements, byte[]  actualread);
	
	/*----------------------------------------------------------------------------*/
	/* Safety Control Function */
	protected static native int  EMGShutDownControl(int CardNumber, byte ctrl);
	
	protected static native int  EMGShutDownStatus(int CardNumber, byte[] sts);
	
	protected static native int  GetInitPattern(int CardNumber, byte patID, int[] pattern);
	
	protected static native int  HotResetHoldControl(int CardNumber, byte enable);
	
	protected static native int  HotResetHoldStatus(int CardNumber, byte[] sts);
	
	protected static native int  IdentifyLED_Control(int CardNumber, byte ctrl);
	
	protected static native int  SetInitPattern(int CardNumber, byte patID, int pattern);
	
	/*----------------------------------------------------------------------------*/
	/* Calibration Function */
	protected static native int  PCI9524AcquireADCalConst(int CardNumber, int Group,  int ADC_Range, int ADC_Speed, int[] CalDate, float[] CalTemp, int[] ADC_offset,  int[] ADC_gain, double[] Residual_offset, double[] Residual_scaling);
	
	protected static native int  PCI9524AcquireDACalConst(int CardNumber, int Channel,  int[] CalDate, float[] CalTemp, byte[] DAC_offset, byte[] DAC_linearity,  float[] Gain_factor);
	
	protected static native int  PCI9524ReadEEProm(int CardNumber, int ReadAddr, byte[] ReadData);
	
	protected static native int  PCI9524ReadRemoteSPI(int CardNumber, int Addr, byte[] RdData);
	
	protected static native int  PCI9524WriteEEProm(int CardNumber, int WriteAddr, byte[] WriteData);
	
	protected static native int  PCI9524WriteRemoteSPI(int CardNumber, int Addr, byte WrtData);
	
	protected static native int  PCIDBAutoCalibrationALL(int CardNumber);
	
	protected static native int  PCIEEPROMCALConstantUpdate(int CardNumber, int bank);
	
	protected static native int  PCILoadCALData(int CardNumber, int bank);
	
	/*----------------------------------------------------------------------------*/
	/* SSI Function */
	protected static native int  SSISourceConn(int CardNumber, int sigCode);
	
	protected static native int  SSISourceDisConn(int CardNumber, int sigCode);
	
	protected static native int  SSISourceClear(int CardNumber);
	
	/*----------------------------------------------------------------------------*/
	/* PWM Function */
	protected static native int  PWMOutput(int CardNumber, int Channel, int high_interval,  int low_interval);
	
	protected static native int  PWMStop(int CardNumber, int Channel);
	
	/*----------------------------------------------------------------------------*/
	/* I2C Function */
	protected static native int  I2CSetup(int CardNumber, int I2C_Port, int I2CConfig,  int I2C_SetupValue1, int I2C_SetupValue2);
	
	protected static native int  I2CControl(int CardNumber, int I2C_Port, int I2C_CtrlParam,  int I2C_CtrlValue);
	
	protected static native int  I2CStatus(int CardNumber, int I2C_Port, int[] I2C_Status);
	
	protected static native int  I2CRead(int CardNumber, int I2C_Port, int I2C_SlaveAddr,  int I2C_CmdAddrBytes, int I2C_DataBytes, int I2C_CmdAddr, int[] I2C_Data);
	
	protected static native int  I2CWrite(int CardNumber, int I2C_Port, int I2C_SlaveAddr,  int I2C_CmdAddrBytes, int I2C_DataBytes, int I2C_CmdAddr, int I2C_Data);
	protected static native int  I2CPhaseShiftSet(int wCardNumber, byte bEnable);
	protected static native int  I2CPhaseShiftCheck(int wCardNumber, byte[] bEnable);
	
	/*----------------------------------------------------------------------------*/
	/* SPI Function */
	protected static native int  SPISetup(int CardNumber, int SPI_Port, int SPI_Config,  int SPI_SetupValue1, int SPI_SetupValue2);
	
	protected static native int  SPIControl(int CardNumber, int SPI_Port, int SPI_CtrlParam,  int SPI_CtrlValue);
	
	protected static native int  SPIStatus(int CardNumber, int SPI_Port, int[] SPI_Status);
	
	protected static native int  SPIRead(int CardNumber, int SPI_Port, int SPI_SlaveAddr,  int SPI_CmdAddrBits, int SPI_DataBits, int SPI_FrontDummyBits,  int SPI_CmdAddr, int[] SPI_Data);
	
	protected static native int  SPIWrite(int CardNumber, int SPI_Port, int SPI_SlaveAddr,  int SPI_CmdAddrBits, int SPI_DataBits, int SPI_FrontDummyBits,  int SPI_TailDummyBits, int SPI_CmdAddr, int SPI_Data);	
}