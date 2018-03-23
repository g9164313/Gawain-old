/*
 * main-xx5.cpp
 *
 *  Created on: 2018年3月1日
 *      Author: qq
 */
#ifdef _MSC_VER
#include "stdafx.h"
#include <Windows.h>
typedef unsigned short uint16_t;
#endif
#include <fstream>
#include <algorithm>
#include <iostream>
#include <vector>
#include <opencv2/opencv.hpp>
#include <ctime>
#include <cmath>

using namespace cv;
using namespace std;

class Token{
private:
public:
	vector<uint16_t> val;
	Token(uint16_t _v[]){
		for(int i=0; i<6; i++){
			val.push_back(_v[i]);
		}
		sort(val.begin(), val.end());
	}
	Token(vector<uint16_t>& vec){
		val = vec;
		sort(val.begin(), val.end());
	}
	bool operator==(const Token& b){
		if (val[0] == b.val[0] &&
			val[1] == b.val[1] &&
			val[2] == b.val[2] &&
			val[3] == b.val[3] &&
			val[4] == b.val[4] &&
			val[5] == b.val[5]
		){ 
			return true;
		}
		return false;
	}
	friend bool operator==(const Token& a, int _v[]){
		int cnt = 0;
		for(int i=0; i<a.val.size(); i++){
			for(int j=0; j<6; j++){
				if(a.val[i]==_v[j]){
					cnt++;
				}
			}
		}
		return (cnt>=4)?(true):(false);
	}
	friend ostream& operator<<(ostream& os, const Token& self){
		char txt[200];
		sprintf(txt,"%02d, %02d, %02d, %02d, %02d, %02d,",
			(int)self.val[0], (int)self.val[1], (int)self.val[2],
			(int)self.val[3], (int)self.val[4], (int)self.val[5]
		);
		os << txt;
		return os;
	}
};

class Combination{
public:
	Combination(int n, int k): N(n), K(k), pool(NULL), total(0L){
		init();
	}
	~Combination(){
		free(pool);
	}
	void list(size_t idx, size_t len){
		_list(cout,idx,len);
	}
	void listAll(){
		_list(cout,0,total);
	}
	friend ostream& operator<<(ostream& os, Combination& self){
		self._list(os,0,self.total);
		return os;
	}
private:
	uint16_t N, K;
	uint16_t* pool;
	size_t total;
	void init(){
		total = comb(N,K);
		pool = (uint16_t*)calloc(K*total,sizeof(uint16_t));

		for(uint16_t k=1; k<=(K-1); k++){
			uint16_t val = k;
			uint16_t* ptr = pool+(k-1)*total;
			for(uint16_t cnt=(N-1);	cnt>=(K-1);	--cnt, ++val){
				size_t len = comb(cnt,K-1);
				for(uint16_t i=0; i<len; i++){
					ptr[i] = val;
				}
				ptr+=len;
			}
		}
		cout<<endl;
	}
	void _list(ostream& os, size_t idx, size_t len){
		for(size_t i=idx; i<len; i++){
			for(uint16_t j=0; j<K; j++){
				char txt[100];
				sprintf(txt,"%02d, ",(int)(pool[j*total+i]));
				os<<txt;
			}
			os<<endl;
		}
	}
	static size_t prodx(size_t cnt){
		size_t res = 1;
		for(size_t i=1; i<=cnt; i++){
			res = res * i;
		}
		return res;
	}
	static size_t comb(size_t NN, size_t KK){
		return prodx(NN) / (prodx(NN-KK)*prodx(KK));
	}
};

static size_t prodx(size_t cnt){
	size_t res = 1;
	for(size_t i=1; i<=cnt; i++){
		res = res * i;
	}
	return res;
}

vector<Token> gatherLotter(const char* name){
	vector<Token> pool;
	ifstream stm1(name);
	string txt;
	size_t cnt = 0;
	while (getline(stm1, txt)){
		cnt++;
		txt.erase(txt.find_last_not_of(" \n\r\t") + 1);
		if (txt[0] == ';' || txt[0] == '/' || txt[0]=='#'){
			continue;
		}

		istringstream stm2(txt);
		uint16_t val[6];
		stm2 >> val[0] >> val[1] >> val[2] >> val[3] >> val[4] >> val[5];

		Token tkn(val);
		pool.push_back(tkn);
		cout<<"gather -->"<<tkn<<endl;
	}
	stm1.close();
	return pool;
}

vector<Token> generateFakeLotter(
	size_t count,
	vector<Token>& avoid
){
	//prepare sequence
	uint16_t seq[47];
	for(int i=0; i<47; i++){
		seq[i] = i+1;
	}

	srand(time(NULL));//creates the seed time

	vector<Token> pool;
	size_t prog = count / 10;
	if(prog==0L){
		prog=1L;
	}

	for(size_t idx=0; idx<count; idx++){

		//disturb sequence
		for(int i=0; i<(47*47); i++){
			int j = i % 47;
			//int k =(rand()%(47-1-j)) + (j+1);
			int k = rand()%47;
			if (j == k){
				i--;
				continue;
			}
			uint16_t tmp = seq[j];
			seq[j] = seq[k];
			seq[k] = tmp;
		}

		//re-select sequence
		uint16_t map[6] = {
			seq[0 * 7 + 3],
			seq[1 * 7 + 3],
			seq[2 * 7 + 3],
			seq[3 * 7 + 3],
			seq[4 * 7 + 3],
			seq[5 * 7 + 3]
		};

		Token tkn(map);
		//cout << "generate -->" << tkn <<endl;
		pool.push_back(tkn);
		for(int i=0; i<avoid.size(); i++){
			if(avoid[i]==tkn){
				//cout<<"skip:"<<tkn<<endl;
				pool.pop_back();
				break;
			}
		}

		if((idx%prog) == (prog-1)){
			cout<<"#"<<endl;
		}
	}
	return pool;
}

void generateFileCSV(
	const char* name,
	vector<Token>& auth,
	int authBeg, int authLen,
	vector<Token>& fake,
	int fakeBeg, int fakeLen
){
	ofstream stm(name,ios::out);
	ostringstream gg;
	if(authLen==0L){
		authLen = auth.size();
	}
	if(authBeg>=0){
		for(size_t i=authBeg; i<authLen; i++){
			stm << auth[i] << "AUTH" << endl;
		}
	}
	if(fakeLen==0L){
		fakeLen = fake.size();
	}
	if(fakeBeg>=0){
		for(size_t i=fakeBeg; i<fakeLen; i++){
			stm << fake[i] << "FAKE" << endl;
		}
	}
	stm.close();
}

void task_generate_comb(){
	Combination c(5,2);
	c.listAll();
}

void task_generate_data(){
	const int AUTH_COUNT=10;
	vector<Token> auth,fake;
	cout<<"gather authority data..."<<endl;
	auth = gatherLotter("./data-lotter/result.txt");
	cout<<endl<<"start to generate fake data..."<<endl;
	fake = generateFakeLotter(AUTH_COUNT*100, auth);
	cout<<endl<<endl<<"dump to CSV file..."<<endl;
	generateFileCSV(
		"../tf-model/official/wide_deep/census_data/lotter.data",
		auth, 0, AUTH_COUNT,
		fake, 0, 0
	);
	fake.clear();
	generateFileCSV(
		"../tf-model/official/wide_deep/census_data/lotter.test",
		auth, AUTH_COUNT, 0,
		fake, -1, 0
	);
	cout<<"Done!!"<<endl;
}

#ifdef _MSC_VER
int _tmain(int argc, _TCHAR* argv[]){
#else
int main(int argc, char* argv[]) {
#endif
	//task_generate_data();
	task_generate_comb();
	return 0;
}
//------------------------------------------//

void generateFigure(const char* name,const int* value){

	Mat node1(3,3,CV_8UC1);
	node1.at<uchar>(0, 0) = value[0] * 5;
	node1.at<uchar>(0, 1) = value[1] * 5;
	node1.at<uchar>(0, 2) = value[0] * 5;
	node1.at<uchar>(1, 0) = value[4] * 5;
	node1.at<uchar>(1, 1) = value[5] * 5;
	node1.at<uchar>(1, 2) = value[2] * 5;
	node1.at<uchar>(2, 0) = value[0] * 5;
	node1.at<uchar>(2, 1) = value[3] * 5;
	node1.at<uchar>(2, 2) = value[0] * 5;
	//cout << node1 << endl;

	Mat node2,node3;

	resize(node1, node2, node1.size()*23, 0, 0, INTER_LINEAR);
	//cout << node2 << endl;

	applyColorMap(node2, node3, COLORMAP_JET);

	imwrite(name, node3);
}


