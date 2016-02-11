package eu.fbk.textProFormat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextProFileFormat {
	
	/* 
	 * write the content of lines in column format
	 */
	public static void writeTextProFile (String [][] lines, String fileNameOut, int nbCol){
		StringBuffer content = new StringBuffer();
		for(int i=0; i<lines.length; i++){
			if(lines[i][0] != null && lines[i][1] != null){
				for(int j=0; j<nbCol; j++){
					if(lines[i][j] == null){
						content.append("_NULL_\t");
					}
					else{
						content.append(lines[i][j]+"\t");
					}
				}
			}
			else if (lines[i][0] != null){
				content.append(lines[i][0]);
			}
			content.append("\n");
		}
		
		
		try {
			File file = new File(fileNameOut);
 
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
 
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			//bw.write(content);
			bw.write(content.toString());
			bw.close();
 
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Read file with one token by line and information in column format (as TextPro format)
	 */
	public static String [][] readFileTextPro (String fileName, int nbCol, boolean onlyCol){
		String [][] lines = null;
		int i=0;
		try{
			InputStream ips=new FileInputStream(fileName); 
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);
			String line;
			
			LineNumberReader reader = new LineNumberReader(new FileReader(fileName));
		    while ((reader.readLine()) != null);
		    int l = 0;
		    
		    lines = new String [reader.getLineNumber()][nbCol];
		    reader.close();
			
			while ((line=br.readLine())!=null){
				String [] tok = new String [nbCol];
				if(!line.matches("^# .*")){
					int j=0;
					String [] col = line.split("\t");
					for(int k=0; k<nbCol && k<col.length; k++){
					//for(String t : line.split("\t")){
						tok[j] = col[k];
						j++;
					}
					
					if(col.length == 1){
						l++;
					}
					else{
						tok[col.length] = Integer.toString(l);
					}
				}
				else if(!onlyCol){
					tok[0] = line;
				}
				if (tok[0] != null){
					lines[i] = tok;
					i++;
				}
			}
			br.close(); 
		}		
		catch (Exception e){
			System.out.println(e.toString());
		}
		return lines;
	}
	
	/*
	 * Read file with one token by line and information in column format (as TextPro format)
	 */
	public static String [][] readFileTextPro (BufferedReader br, int nbCol, boolean onlyCol){
		String [][] lines = null;
		int i=0;
		try{
			//String line;
			int nbLine = 0;
			
			String strLine;
			ArrayList<String> ans= new ArrayList<String>();
	         // Read rows
	        while ((strLine = br.readLine()) != null) {
	              ans.add(strLine);
	         } 
			
			/*LineNumberReader reader = new LineNumberReader(br);
		    while ((br.readLine()) != null){
		    	nbLine ++;
		    }*/
		    int l = 0;
		    
		    //lines = new String [reader.getLineNumber()][nbCol];
		    //reader.close();
		    lines = new String [ans.size()][nbCol];
			
		    for(String line : ans){
			//while ((line=br.readLine())!=null){
				String [] tok = new String [nbCol];
				if(!line.matches("^# .*")){
					int j=0;
					String [] col = line.split("\t");
					for(int k=0; k<nbCol && k<col.length; k++){
					//for(String t : line.split("\t")){
						tok[j] = col[k];
						j++;
					}
					
					if(col.length == 1){
						l++;
					}
					else{
						tok[col.length] = Integer.toString(l);
					}
				}
				else if(!onlyCol){
					tok[0] = line;
				}
				if (tok[0] != null){
					lines[i] = tok;
					i++;
				}
			}
			br.close(); 
		}		
		catch (Exception e){
			System.out.println(e.toString());
		}
		return lines;
	}
	
	
	
	public static String getDate(String file){
		String dateString = "";
		try {
			BufferedReader br=new BufferedReader(new FileReader(file));
			Boolean date = false;
			int i = 0;
			while (!date && i<10){
				String line = br.readLine();
				if (line.startsWith("# DATE") || line.startsWith("# creationtime") || line.startsWith("# CREATIONTIME") || line.startsWith("# date")){
					
					Pattern p = Pattern.compile(": (.+)$");
					Matcher m = p.matcher(line);
					
					if(m.find()){
						dateString = m.group(1);
						date = true;
					}
				}
				i++;
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*if(dateString.equals("")){
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			Date date = new Date();
			dateString = dateFormat.format(date).toString();
		}*/
		return dateString;
	}
	
	public static String getHeader(String file){
		String header = "";
		try {
			BufferedReader br=new BufferedReader(new FileReader(file));
			Boolean date = false;
			int i = 0;
			while (!date && i<10){
				String line = br.readLine();
				if (line.startsWith("# ")){
					header += line+"\n";
				}
				i++;
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return header;
	}
	
	public static String getDateFromLine(String dateLine){
		String dateString = "";
		Pattern p = Pattern.compile("(DATE|CREATIONTIME|date|creationtime): (.+)$");
		Matcher m = p.matcher(dateLine);
					
		if(m.find()){
			dateString = m.group(2);
		}
		return dateString;
	}
	
	
}
