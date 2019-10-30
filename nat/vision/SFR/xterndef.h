
/******************************************************************************/
/*              arrays and file pointers                                      */
/******************************************************************************/
FILE *g_mtfout;
char g_debug_file_name[80];
char image_filename[80];
char data_filename[80];
int g_scan_image_file_id;
int g_ofd; 

unsigned char *g_image_array;

/******************************************************************************/
/*      image, test pattern and patch locations and dimensions                */
/******************************************************************************/

short g_input_column_start;
short g_input_last_column;
short g_input_row_start;
short g_input_last_row;
short g_input_width;
short g_input_height;

unsigned int g_total_image_width;
unsigned int g_total_image_height;
unsigned int g_bytes_per_pixel;
unsigned int g_max_pixel_value;

int g_test_pattern_xul;
int g_test_pattern_yul;
int g_test_pattern_xlr;
int g_test_pattern_ylr;
int g_target_res;

double g_target_width_in_mm;
double g_target_height_in_mm;

double g_target_width;
double g_target_height;

#define MAX_PROBLEMS 60
#define MAX_LINE_LENGTH 132

char g_problems[MAX_PROBLEMS][MAX_LINE_LENGTH];
short g_problem_count;
char g_problem_type[MAX_PROBLEMS];
short g_IQS_problem_count;


/******************************************************************************/
/*              things read in                                                */
/******************************************************************************/

unsigned int g_scan_rows;
unsigned int g_scan_cols;
unsigned int g_scan_header;
unsigned short g_bps;
unsigned short g_photometric;

double g_ppi;

int g_version;
unsigned char g_center;
unsigned char g_debug;
unsigned char g_streamline_args;
unsigned char g_reversepolarity;
unsigned char g_extended;
unsigned char g_pgm;
unsigned char g_autorefine;
unsigned char g_nocompare;

