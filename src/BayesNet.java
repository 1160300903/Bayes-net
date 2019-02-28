import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BayesNet {
	int numOfNode;
	int[][] graph;
	List<Node> nodeList;
	Map<String,Integer> nameToIndex=new HashMap<String,Integer>();
	double[] jointPro;
	public BayesNet(String path) {
		File file = new File(path);
		BufferedReader fileReader=null;
		String line=null;
		try {
			fileReader=new BufferedReader(new FileReader(file));
			line=fileReader.readLine();
			numOfNode=Integer.parseInt(line);
			line=fileReader.readLine();
			line=fileReader.readLine();
			nodeList=new ArrayList<Node>();
			int count=0;
			while(!line.equals("")) {
				String[] names=line.split("\\s+");
				for(int i=0;i<names.length;i++) {
					nodeList.add(new Node(names[i]));
					nameToIndex.put(names[i], count);
					count++;
				}
				line=fileReader.readLine();
			}
			graph=new int[numOfNode][numOfNode];
			for(int i=0;i<numOfNode;i++) {
				line=fileReader.readLine();
				String[] way=line.split("\\s+");
				for(int j=0;j<numOfNode;j++) {
					graph[i][j]=Integer.parseInt(way[j]);
				}
			}
			line=fileReader.readLine();
			int i=0;
			while(true) {
				line=fileReader.readLine();
				if(line==null) {
					break;
				}
				if(line.equals("")) {
					i++;
					continue;
				}
				nodeList.get(i).addPro(line);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		jointPro=new double[1<<numOfNode];
		for(int i=0;i<jointPro.length;i++) {
				jointPro[i]=1.0;
		}
		for(int i=0;i<numOfNode;i++) {
			int[] bit;
			List<Integer> father=getFather(i);
			Node node=nodeList.get(i);
			int[] valueOfFather=new int[father.size()];
			for(int j=0;j<jointPro.length;j++) {
				bit=intToBitArray(j);
				int m=0;
				for(int k:father) {
					valueOfFather[m]=bit[k];
					m++;
				}
				int value=bitArrayToInt(valueOfFather);
				if(bit[i]==0)
					jointPro[j]*=node.getPro(value)[1];
				if(bit[i]==1)
					jointPro[j]*=node.getPro(value)[0];
			}
		}
	}
	private ArrayList<Integer> getFather(int i){
		ArrayList<Integer> father=new ArrayList<>();
		for(int j=0;j<numOfNode;j++) {
			if(graph[j][i]==1)
				father.add(j);
		}
		return father;
	}
	private int[] intToBitArray(int i) {
		int[] a=new int[numOfNode];
		for(int j=0;j<numOfNode;j++) {
			int k=i%2;
			i=i/2;
			a[numOfNode-1-j]=k;
		}
		return a;
	}
	private int bitArrayToInt(int[] array) {
		int k=1;
		int sum=0;
		for(int i=0;i<array.length;i++) {
			sum+=k*array[array.length-1-i];
			k=k<<1;
		}
		return sum;
	}
	public void coumputeProFromFile(String path) {
		File file=new File(path);
		try {
			BufferedReader reader=new BufferedReader(new FileReader(file));
			String line=reader.readLine();
			while(line!=null) {
				if(line.equals("")) {
					line=reader.readLine();
					continue;
				}
				double[] result=proInference(line);
				line=reader.readLine();
				System.out.println(result[0]+" "+result[1]);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * probabilistic infer
	 * @param expression:计算概率的表达式P(Burglar | Alarm=true)
	 * @return 概率
	 */
	public double[] proInference(String expression) {
		int targetVar=-1;
		int[] conditionalVar=new int[numOfNode];
		for(int i=0;i<numOfNode;i++)
			conditionalVar[i]=-1;
		String[] var=expression.split("[\\|,]");
		for(int i=0;i<var.length;i++) {
			var[i]=var[i].trim();
			if(i==0)
				var[i]=var[i].substring(2, var[i].length());
			if(i==var.length-1)
				var[i]=var[i].substring(0, var[i].length()-1);
			String[] tmp=var[i].split("=");
			if(tmp.length==1)
				targetVar=nameToIndex.get(tmp[0]);
			else {
				if(tmp[1].equals("true"))
					conditionalVar[nameToIndex.get(tmp[0])]=1;
				else if(tmp[1].equals("false"))
					conditionalVar[nameToIndex.get(tmp[0])]=0;
			}
		}
		//Collections.sort(targetVar);
		double[] result=new double[2];
		a:for(int i=0;i<1<<numOfNode;i++) {
			int[] a=intToBitArray(i);
			for(int j=0;j<numOfNode;j++)
				if(conditionalVar[j]!=-1&&conditionalVar[j]!=a[j])
					continue a;
			if(a[targetVar]==0)
				result[1]+=jointPro[i];
			else if(a[targetVar]==1)
				result[0]+=jointPro[i];
		}
		double sum=0;
		for(int i=0;i<result.length;i++)
			sum+=result[i];
		for(int i=0;i<result.length;i++)
			result[i]=result[i]/sum;
		return result;
	}
	@Override
	public String toString() {
		String s="";
		for(Node node:nodeList) {
			s+=node.toString();
		}
		return s;
	}
	public static void main(String[] args) {
		BayesNet b=new BayesNet("burglarnetwork.txt");
		b.coumputeProFromFile("burglarqueries.txt");
	}
}
class Node{
	String name;
	List<Double[]> pro=new ArrayList<>();
	public Node(String name) {
		this.name=name;
	}
	public Double[] getPro(int i) {
		return pro.get(i);
	}
	public void addPro(String line) {
		String[] s=line.split("\\s+");
		if(s.length!=2) {
			try {
				throw new Exception();
			} catch (Exception e) {
				System.out.println("概率输入错误");
				e.printStackTrace();
			}
		}
		Double[] a=new Double[2];
		for(int i=0;i<2;i++) {
			a[i]=Double.parseDouble(s[i]);
		}
		pro.add(a);
	}
	@Override
	public String toString() {
		String a="";
		for(Double[] s :pro) {
			a+=String.valueOf(s[0])+" "+String.valueOf(s[1])+"\n";
		}
		return a;
	}
}