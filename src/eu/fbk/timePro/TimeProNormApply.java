/**
 * Time expression normalization using timenorm (Bethard, 2013)
 * Class used by TimePro
 * @author Anne-Lyse Minard
 * @version 2.6 (2016-06-10)
 * if no dct, get a date in the first 2 sentences
 */


package eu.fbk.timePro;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.fbk.textProFormat.*;

import java.io.*;

public class TimeProNormApply {


	public static int colTimex = 7;
	public static int colTimexID = 9;
	public static int colValTimex = 10;
	public static int colRules = 6;
	public static int colAnchor = 11;
	public static int colLemma = 2;
	public static int colBeginPoint = 12;
	public static int colEndPoint = 13;
	public static int colPOS = 1;
	public static int colToken = 0;
	public static int colSent = 8;
	
	public static int colNbSent = 1;
	
	public static int nbCol = 14;
	
	public static boolean dctIsTodayDate = false;
	
	public static HashMap<String,String> numbers = new HashMap<String,String> ();
	
	
	/**
	 * Fill the hash numbers: key --> word ; val --> number
	 * @return
	 */
	public static void fill_hash_numbers(){
		numbers.put("one","1");
		numbers.put("two","2");
		numbers.put("three","3");
		numbers.put("four","4");
		numbers.put("five","5");
		numbers.put("six","6");
		numbers.put("seven","7");
		numbers.put("eight","8");
		numbers.put("nine","9");
		numbers.put("ten","10");
	}
	
	
	/**
	 * Get the index of the first and the last words in the sentence of a given token
	 * @param lines
	 * @param i index of the given token
	 * @return an array containing two elements: the index of the first word and the index of the second word
	 */
	public static int[] getSentence (String[][] lines, int i){
		
		int begin = 0;
		int end = 0;
		String numSent = lines[i][colSent];
		int [] sent = new int[2];
		for (int k=0; k<lines.length; k++){
			if (lines[k] != null && lines[k][colSent] != null && lines[k][colSent].equals(numSent)){
				if(begin == 0){
					begin = k;
				}
			}
			else if(begin != 0){
				end = k;
				break;
			}
		}
		sent[0] = begin;
		sent[1] = end;
		
		return sent;
	}
	
	public static Boolean hasPastBefore (String [][] lines_bef_timex){
		boolean verbBef = false;
		for (int i = lines_bef_timex.length-1; i>=0; i--){
			if(lines_bef_timex[i] != null && lines_bef_timex[i][0] != null && lines_bef_timex[i][colPOS].startsWith("V")){
				if (lines_bef_timex[i][colPOS].matches("VVD") && !verbBef){
					return true;
				}
				else if(!lines_bef_timex[i][colPOS].matches("VVG")){
					verbBef = true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Given the tokens before and after a timex, identify if the timex has a beginPoint and/or a endPoint
	 * @param lines_timex : token of the timex
	 * @param lines_afTimex : token after the timex
	 * @param lines_befTimex : token before the timex
	 * @return an array of Boolean containing two elements: hasBeginPoint and hasEndPoint
	 */
	public static Boolean [] hasBegEndPoint(String [][] lines_timex, String[][] lines_afTimex, String[][] lines_befTimex){
		Boolean hasEndPoint = false;
		Boolean hasBeginPoint = false;
		// Search if the timex contains one word indicating if it has a endPoint or a beginPoint
		for(int k=0; k<lines_timex.length; k++){
			if (lines_timex[k] != null && lines_timex[k][colLemma] != null){
				Pattern p1 = Pattern.compile(
						"^(ago|last|latest|recent|past)$");
				Matcher m1 = p1.matcher(lines_timex[k][colLemma]);
				
				if(m1.find()){
					hasEndPoint = true;
				}
				
				Pattern p2 = Pattern.compile(
						"^(next|ahead|come|within|additional)$");
				Matcher m2 = p2.matcher(lines_timex[k][colLemma]);
				
				if(m2.find()){
					hasBeginPoint = true;
				}
			}
		}
	
		// Search in the tokens after the timex if any indicating if the timex has a endPoint or a beginPoint
		for(int k=0; k<lines_afTimex.length; k++){
			if(lines_afTimex[k] != null && lines_afTimex[k][colLemma] != null){
				Pattern p1 = Pattern.compile(
						"^(end|earlier|before)$");
				Matcher m1 = p1.matcher(lines_afTimex[k][colLemma]);
				
				if(m1.find()){
					hasEndPoint = true;
				}
				
				Pattern p2 = Pattern.compile(
						"^(later|after)$");
				Matcher m2 = p2.matcher(lines_afTimex[k][colLemma]);
				
				if(m2.find()){
					hasBeginPoint = true;
				}	
			}
			else{
				break;
			}
		}
		
		// Search in the tokens before the timex if any indicating if the timex has a beginPoint
		for(int k=0; k<lines_befTimex.length; k++){
			if(lines_befTimex[k] != null && lines_befTimex[k][colLemma] != null){
				Pattern p2 = Pattern.compile(
						"^(within|after)$");
				Matcher m2 = p2.matcher(lines_befTimex[k][colLemma]);
				
				if(m2.find()){
					hasBeginPoint = true;
				}	
			}
		}
		
		Boolean [] point = new Boolean[2];
		point[0] = hasBeginPoint;
		point[1] = hasEndPoint;
		
		return point;
	}
	
	
	
	/**
	 * Detect if a timex has an anchorTime
	 * @param lines_timex : tokens in the timex
	 * @param typeTimex
	 * @return boolean
	 */
	public static Boolean hasAnchorTime (String [][] lines_timex, String typeTimex){
		Boolean hasAnchorTime = true;
		for(int k=0; k<lines_timex.length; k++){

			if(typeTimex.equals("DATE")){
				if(lines_timex[k][colRules].equals("_YY_")){
					hasAnchorTime = false;
				}
				else{
					Pattern p1 = Pattern.compile(
							"^(century|nineties|eigthies|seventies|sixties|(mid-[1-9][0-9]|[1-2][0-9][0-9][0-9]s?)|([1-2][0-9][0-9][0-9]s)|('[1-9]0s))\\s?$");
					Matcher m1 = p1.matcher(lines_timex[k][colToken]);
					
					if(m1.find()){
						hasAnchorTime = false;
					}
				}
				if(lines_timex[k][colLemma].matches("(quarter|fiscal|period)")){
					hasAnchorTime = true;
				}
			}
			
			if(typeTimex.equals("TIME")){
				if(lines_timex[k][colRules].matches("^('[1-9][0-9]|[1-2][0-9][0-9][0-9])\\s?$")){
					hasAnchorTime = false;
				}
			}
		}
		
		return hasAnchorTime;
	}
	
	/**
	 * get the last timex id
	 */
	private static int getCptTimexId (String [][] lines){
		int lastTimexId = 0;
		boolean find = false;
		int j=lines.length-1;
		while(!find && j>0){
			if(lines[j] != null && lines[j][colTimexID] != null && lines[j][colTimexID].startsWith("tmx")){
				lastTimexId = Integer.parseInt(lines[j][colTimexID].replace("tmx", ""));
				find = true;
			}
			j--;
		}
		
		return lastTimexId;
	}
	
	
	/**
	 * Main function
	 * For each timex:
	 * - identify if it has an anchorTime and/or a beginPoint and/or an endPoint and their values
	 * - send the timex and the anchorTime to TimeNorm, which return the normalized value of the timex
	 * - if TimeNorm failed, compute a value thanks to simple rules
	 * @param lines: array containing one array by token
	 * @param dct: document creation time
	 * @return lines with new columns (value, anchorTime, beginPoint, endPoint)
	 * @throws ParseException
	 * @throws InterruptedException 
	 */
	public static String[][] addTimexValue (String[][] lines, String dct) throws ParseException, InterruptedException{
		int cptTimexId = getCptTimexId(lines);
		for (int i=0; i<lines.length; i++){
			if (lines[i] != null && lines[i][colTimex] != null && lines[i][colTimex].startsWith("B-")){
				int j = i+1;
				while (j<lines.length && lines[j] != null && lines[j][colTimex] != null && lines[j][colTimex].startsWith("I-")){
					j++;
				}
				if(j<lines.length && lines[j] != null && lines[j][colTimex] != null && lines[j][colTimex].startsWith("B-")){
					if(lines[j][colTimex].equals("B-DATE") && lines[i][colTimex].equals("B-DATE") && lines[j][colRules].equals("_YY_")){
						lines[j][colTimex] = "I-DATE";
						lines[j][colTimexID] = lines[i][colTimexID];
					}
				}
				else if(j<lines.length && lines[j] != null && lines[j][colTimex] != null 
						&& lines[i][colTimex].equals("B-DATE") && lines[j][colRules].equals("_YY_")){
					lines[j][colTimex] = "I-DATE";
					lines[j][colTimexID] = lines[i][colTimexID];
				}
			}
			else if(lines[i] != null && lines[i][colTimex] != null && lines[i][colTimex].equals("O")){
				if(lines[i][0].matches("^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]$")){
					lines[i][colTimex] = "B-DATE";
					cptTimexId++;
					lines[i][colTimexID] = "tmx"+Integer.toString(cptTimexId);
				}
			}
		}
		
		
		for (int i=0; i<lines.length; i++){
			
			boolean isFuture = false;
			
			// If first token of a timex
			if(lines[i] != null && lines[i][colTimex] != null && lines[i][colTimex].startsWith("B-")){
				boolean hasAnchorTime = true;
				boolean hasBeginPoint = false;
				boolean hasEndPoint = false;
				String pointID [] = new String [2];
				int [] sentence = getSentence(lines,i);
				
				String anchorTime = dct;
				
				
				String anchorTimeID = "";
				String newAnchorTime = "";

				String anchorTimeThat = "";
				String timex = lines[i][colToken];
				int j = i+1;
				int numTok = 1;
				while (j<lines.length && lines[j] != null && lines[j][colTimex] != null && lines[j][colTimex].startsWith("I-")){
					timex += " "+lines[j][colToken];
					j++;
					numTok++;
				}
				
				if(timex.matches("^[1-9]([0-9])?\\.[0-9]([0-9])? [1-9]([0-9])?$")){
					for (int k=i; k<i+numTok; k++){
						lines[k][colTimex] = "O";
						lines[k][colTimexID] = "O";
					}
				}
				else{
				/* Build a new timex in the case of coordination
				 * April 24 and 25
				 * Monday and Tuesday nights
				 *  */
				String newTimex = "";
				
				
				if(i+3<lines.length && lines[i+2] != null && lines[i+2][colTimex] != null && lines[i+1][colPOS] != null
						&& lines[i+2][colTimex].startsWith("B-") && lines[i+1] != null 
						&& (lines[i+1][colPOS].equals("CJC") || lines[i+1][colPOS].equals("TO0"))
						&& lines[i+3] != null && lines[i+3][colTimex] != null && lines[i+3][colTimex].startsWith("I-")){
					if(lines[i+2][colRules].equals(lines[i][colRules])){
						newTimex = lines[i][colToken];
						int k = i+3;
						while(lines[k][colTimex].startsWith("I-")){
							newTimex += " "+lines[k][colToken];
							k++;
						}
					}
				}
				
				
				
				if(i>3 && lines[i-2] != null && lines[i-2][colTimex] != null &&  lines[i-1][colPOS] != null
						&& lines[i-2][colTimex].startsWith("I-") && lines[i-1] != null 
						&& (lines[i-1][colPOS].equals("CJC") || lines[i-1][colPOS].equals("TO0"))){
					
					if (lines[i-2][colRules].equals(lines[i][colRules])){
						
						if(lines[i-3] != null && lines[i-3][colTimex] != null && lines[i-3][colTimex].startsWith("B-")){
							newTimex = lines[i-3][colToken]+" "+lines[i][colToken];
						}
						else if(lines[i-3] != null && lines[i-3][colTimex] != null && lines[i-3][colTimex].startsWith("I-")){
							int k = i-3;
							while(k>0 && !lines[k][colTimex].startsWith("B-")){
								newTimex = lines[k][colToken]+" "+newTimex;
								k--;
							}
							newTimex = lines[j][colToken]+" "+newTimex;
							newTimex += lines[i][colToken];
						}
					}
				}
				
				if(!newTimex.equals("")){
					timex = newTimex;
				}
				
				/*
				 * Complex timex with time: "10:35 a.m. (0735 GMT) Friday"
				 */
				if((timex.contains("a.m.") || timex.contains("p.m.") || timex.contains("morning") 
						|| timex.contains("evening") || timex.contains("night")) 
						&& !timex.contains("day")){
					int numSent = Integer.parseInt(lines[i][colSent]);
					newTimex = timex;
					int k = i;
					while(lines[k] != null && lines[k][colSent] != null && lines[k][colSent].equals(Integer.toString(numSent))){
						if(lines[k][colRules] != null && lines[k][colRules].equals("_DAY_")){
							newTimex += " "+lines[k][colLemma];
							break;
						}
						k++;
					}
				}
				if(!newTimex.equals("")){
					timex = newTimex;
				}
				
				
				/*
				 * Search for the anchorTime + beginPoint and endPoint
				 */
				
				if (lines[i][colTimex].startsWith("B-DATE") || lines[i][colTimex].startsWith("B-TIME")){
					
					hasAnchorTime = hasAnchorTime(Arrays.copyOfRange(lines,i,i+numTok),"DATE");
					anchorTimeID = getAnchorTime(timex, anchorTime, lines, sentence, i, i+numTok);
					if(anchorTimeID.equals("") && hasAnchorTime){
						anchorTimeID = "tmx0";
					}
					
					
					/*
					 * search anchor time if timex contains "that"
					 */
					
					if(timex.contains("that")){
						Boolean keepAnchorTime = false;
						for (int k=i ; k<j; k++){
							if (lines[i][colRules].equals("_DAY_")){
								keepAnchorTime = true;
							}
						}
						if(!keepAnchorTime){
							int numSent = Integer.parseInt(lines[i][colSent]);
							if(numSent > 1){
								for (int k=i; k>0; k--){
									if(lines[k] != null && lines[k][colSent] != null){
										if(lines[k][colSent].equals(Integer.toString(numSent)) || lines[k][colSent].equals(Integer.toString(numSent-1))){
											if(lines[k][colTimex].endsWith("DATE") && lines[k][colValTimex] != null){
												anchorTimeThat = lines[k][colValTimex];
												anchorTimeID = lines[k][colTimexID];
												if(anchorTimeThat.length() < 10){
													anchorTimeThat+="-01";
												}
											}
										}
										else{
											break;
										}
									}
								}
							}
						}
					}
					
					/*
					 * Modify the anchorTime in case of future date
					 */

					
					for (int s=sentence[0]; s<sentence[1]; s++){
						if(lines[s] != null && lines[s][colLemma] != null){
							if(lines[s][colLemma].equals("will")){
							//if(lines[s][colLemma].equals("will") || !hasPastBefore(Arrays.copyOfRange(lines,sentence[0],i))){ //change 10/06/16
								isFuture = true;
							}
							if(s == (i-1)){
								Pattern p2 = Pattern.compile(
										"^(until)|(by)|(on)$");
								Matcher m2 = p2.matcher(lines[s][colLemma]);
								
								if(m2.find() && !hasPastBefore(Arrays.copyOfRange(lines,sentence[0],i))){
									//isFuture = true;
								}
							}
						}
					}
					if(!timex.matches(".*[0-9][0-9][0-9][0-9].*") 
							&& (timex.contains("day") || timex.matches(".* [0-9][0-9]\\.? ?")
									|| timex.matches(".*(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[^ ]* [0-9].*")
									|| timex.matches("(mid-)?(January|February|March|April|May|June|July|August|September|October|November|December)"))){
						//isFuture = true;
						
						/*if(sentence[0] <= i-2){
							int k = i-1;
							while(k>i-6 && k>=sentence[0]){
								if(lines[k] != null && lines[k][colLemma] != null && lines[k][colLemma].matches("(before|previous|past|last|since)")){
									//isFuture = false;
								}
								k--;
							}
						}
						if(timex.matches(".*(last|early|earlier|ago).*")){
							//isFuture = false;
						}
						if(lines[i-1][colPOS] != null && lines[i-1][colPOS].startsWith("V") 
								&& lines[i-1][colToken].endsWith("d")){
							//isFuture = false;
						}*/
					}
					
					/*if(isFuture){
						newAnchorTime = anchorTime; 
						if(timex.contains("day")){
							newAnchorTime = compute_value(timex,anchorTime,anchorTime,Arrays.copyOfRange(lines,i,i+numTok),"addWeek");
						}
						else if (! timex.contains("that") && !timex.contains("this")){
							newAnchorTime = compute_value(timex,anchorTime,anchorTime,Arrays.copyOfRange(lines,i,i+numTok),"addYear");
						}
					}*/
					
					if(!anchorTimeThat.equals("") && anchorTimeThat.matches("[0-9]+-[0-9]+-[0-9]+")){
						anchorTime = anchorTimeThat;
					}
				}
				
				else if(lines[i][colTimex].startsWith("B-TIME")){
					hasAnchorTime = hasAnchorTime(Arrays.copyOfRange(lines, i, i+numTok),"TIME");
				}
				
				else if(lines[i][colTimex].startsWith("B-SET")){
					hasAnchorTime = false;
				}
				
				else if(lines[i][colTimex].startsWith("B-DURATION")){
					int min = 0;
					if(i>3){
						min = i-3;
					}
					Boolean [] point = hasBegEndPoint(Arrays.copyOfRange(lines,i,i+numTok), 
							Arrays.copyOfRange(lines,i+numTok,i+numTok+5), Arrays.copyOfRange(lines,min,i));
					hasBeginPoint = point[0];
					hasEndPoint = point[1];
					
					pointID = getBegEndPoint(timex, anchorTime, lines, sentence, i, i+numTok);
				}
				

				/*
				 * TimeNorm
				 */
				
				if((timex.contains(".30") || timex.contains(".15") || timex.contains(".45") || timex.contains(".00")) && !timex.contains("am") && !timex.contains("pm")){
					Pattern p = Pattern.compile("(.* )([0-9]+)\\.([0-9]+.*)$");
					Matcher m = p.matcher(timex);
					if(m.find()){
						timex = m.replaceFirst(m.group(1)+m.group(2)+":"+m.group(3));
						if(Integer.parseInt(m.group(2)) < 7 || Integer.parseInt(m.group(2)) == 12){
							timex = timex+"pm";
						}
						else{
							timex = timex+"am";
						}
					}
					else{
						timex = timex+"am";
					}
				}
				
				if(timex.matches("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[^ ]* [0-9][0-9]-[0-9][0-9]$")){
					Pattern p = Pattern.compile("((Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[^ ]* [0-9][0-9])-[0-9][0-9]$");
					Matcher m = p.matcher(timex);
					if(m.find()){
						timex = m.replaceFirst(m.group(1));
					}
				}
				
				if(!anchorTimeID.equals("tmx0") && !anchorTimeID.equals("")){
					anchorTime = getValTimexID(anchorTimeID, lines, anchorTime);
				}
				anchorTime = checkDate(anchorTime);
								
				if(anchorTime.equals("")){
					anchorTime = dct;
				}
				
				
				
				String listValuesString = "";
				if(checkTimex(timex)){
					long start = System.currentTimeMillis();
					long end = start + 240; // 60 seconds * 1000 ms/sec
					while (System.currentTimeMillis() < end)
					{
						listValuesString = TestTimeNorm_parseAll.getTimeNorm(timex, anchorTime);
					}
				}
				else{
					listValuesString = "FAIL;";
				}
				
				String[] listValues = listValuesString.split(";");
				String value = listValues[0];
				
				//System.out.println(listValuesString);
				//System.out.println(listValues[0]);
				
				if(listValues.length > 1){
					for (String v : listValues){
						if (v.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]") 
								&& value.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]") && isFuture){
							SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd" ); 
							java.util.Date d1 = sdf.parse( v );  	
							java.util.Date d2 = sdf.parse( value );  
							if (d1.after(d2)){
								value = v;
							}
						}
						//added 10/06/16
						else if(v.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9](-[0-9][0-9])?") 
								&& value.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9](-[0-9][0-9])?")){
							SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd" ); 
							String newV = v;
							String newValue = value;
							if(v.matches("^[0-9][0-9][0-9][0-9]-[0-9][0-9]$")){
								newV += "-01";
							}
							if(value.matches("^[0-9][0-9][0-9][0-9]-[0-9][0-9]$")){
								newValue += "-01";
							}
							java.util.Date d1 = sdf.parse( newV );  	
							java.util.Date d2 = sdf.parse( newValue );  
							java.util.Date dctDate = sdf.parse ( dct );
							if(isFuture && d1.after(d2)){
								value = v;
							}
							else{
								if(d1.after(dctDate) && d2.before(dctDate) && d1.getTime() - dctDate.getTime() < dctDate.getTime() - d2.getTime()){
									value = v;
								}
								else if(d1.before(dctDate) && d2.after(dctDate) && d2.getTime()-dctDate.getTime() > dctDate.getTime() - d1.getTime()){
									value = v;
								}
							}
						}
						/*if(value.matches("[0-9][0-9][0-9][0-9]")
								&& v.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]")){
							value = v;
						}*/
					
					}
				}
				
				String listValuesBisString = "";
				
				if (! newAnchorTime.equals("") && anchorTimeThat.equals("")){
					newAnchorTime = checkDate(newAnchorTime);
					long start = System.currentTimeMillis();
					long end = start + 120; // 60 seconds * 1000 ms/sec
					while (System.currentTimeMillis() < end)
					{
						listValuesBisString = TestTimeNorm_parseAll.getTimeNorm(timex, newAnchorTime);
					}
				}
				String[] listValuesBis = listValuesBisString.split(";");
				String valueBis = listValuesBis[0];
				
				
				/*
				 * Modify the time expression if TimeNorm failed
				 */
				if (value.equals("FAIL")){
					newTimex = replaceFailValue(timex, Arrays.copyOfRange(lines,i,i+numTok));
					
					if(!newTimex.equals("")){
						String listValueString = "";
						long start = System.currentTimeMillis();
						long end = start + 120; // 60 seconds * 1000 ms/sec
						while (System.currentTimeMillis() < end)
						{
							listValueString = TestTimeNorm_parseAll.getTimeNorm(newTimex, anchorTime);
						}
						value = listValueString.split(";")[0];
					}
				}
				
				/*
				 * If two values were obtained, compare them and keep the closest from the DCT
				 */
				String newValue = value;
				if(!valueBis.equals("") && valueBis.matches("[0-9]+-[0-9]+(-[0-9]+(T.*)?)?")
					&& !value.equals("") && value.matches("[0-9]+-[0-9]+(-[0-9]+(T.*)?)?")){
					String valueBisReFormat = valueBis;
					String valueReFormat = value;
					String anchorTimeReFormat = anchorTime;
					if(valueBisReFormat.contains("T") && valueBisReFormat.length()>10){
						valueBisReFormat = valueBisReFormat.substring(0,10);
					}
					if(valueReFormat.contains("T") && valueReFormat.length()>10){
						valueReFormat = valueReFormat.substring(0,10);
					}
					if(anchorTimeReFormat.contains("T") && anchorTimeReFormat.length()>10){
						anchorTimeReFormat = anchorTimeReFormat.substring(0,10);
					}
					
					Date date1 = new Date();
					Date date2 = new Date();
					Date dateDCT = new Date();
					
					date1 = new SimpleDateFormat(getFormat(valueBisReFormat)).parse(valueBisReFormat);
					//Date date1 = new Date(valueBis);
					date2 = new SimpleDateFormat(getFormat(valueReFormat)).parse(valueReFormat);
					dateDCT = new SimpleDateFormat(getFormat(anchorTimeReFormat)).parse(anchorTimeReFormat);
				
					if((dateDCT.after(date1) && dateDCT.after(date2) 
						&& dateDCT.getTime() - date1.getTime() < dateDCT.getTime() - date2.getTime())
						|| (dateDCT.after(date2) && dateDCT.before(date1) 
								&& dateDCT.getTime() - date2.getTime() > date1.getTime() - dateDCT.getTime())
						|| (dateDCT.before(date1) && dateDCT.before(date2)
								&& dateDCT.getTime() - date1.getTime() < dateDCT.getTime() - date2.getTime())
						){
						newValue = valueBis;
					}
					if (date2.equals(dateDCT)){
						newValue = value;
					}
					else if (date1.equals(dateDCT)){
						newValue = valueBis;
					}
				}
				else if (!valueBis.equals("")){
					newValue = valueBis;
				}
				value = newValue;
				
				/*
				 * if DATE or TIME and value = P.*
				 * compute of a new value
				 */
				if(value.startsWith("P") && !value.contains("REF") && !lines[i][colTimex].equals("B-DURATION") 
						&& !lines[i][colTimex].equals("B-SET")){
					if(lines[i+numTok] != null && lines[i+numTok][colToken] != null 
							&& lines[i+numTok][colToken].toLowerCase().matches("(before)|(after)|(of)")){
						value = compute_value(timex, anchorTime, value, Arrays.copyOfRange(lines, i, i+numTok), "bef/af");
						
					}
					else if(lines[i-1] != null && lines[i-1][colToken] != null && lines[i-1][colToken].toLowerCase().equals("in")){
						value = compute_value(timex, anchorTime, value, Arrays.copyOfRange(lines, i, i+numTok),"in");
					}
					else if((lines[i+numTok] != null && lines[i+numTok][colToken] != null 
							&& lines[i+numTok][colToken].matches("earlier"))
							|| lines[i-1] != null && lines[i-1][colToken] != null && lines[i-1][colToken].toLowerCase().equals("last")){
						value = compute_value(timex, anchorTime, value, Arrays.copyOfRange(lines, i, i+numTok), "early");
						
					}
					else if((lines[i+numTok] != null && lines[i+numTok][colToken] != null 
							&& lines[i+numTok][colToken].matches("later")) 
							|| lines[i-1] != null && lines[i-1][colToken] != null && lines[i-1][colToken].toLowerCase().equals("next")){
						value = compute_value(timex, anchorTime, value, Arrays.copyOfRange(lines, i, i+numTok), "late");
						
					}
				}
				
				if(timex.contains("quarter") && lines[i+numTok] != null && lines[i+numTok][colToken] != null && lines[i+numTok][colToken].matches("end.*")){
					value = compute_value(timex, anchorTime, value, Arrays.copyOfRange(lines, i, lines.length), "quarter");
				}
				
				// if no value was returned by TimeNorm, compute one (by default "XXXX-XX-XX")
				if (value.equals("FAIL")){
					value = "XXXX-XX-XX";
					Boolean hasDay = false;
					Boolean hasMonth = false;
					Boolean hasYear = false;
					Boolean hasWeek = false;
					
					for(int k=i; k<i+numTok; k++){
						if(lines[k][colRules].equals("_MONTH_")){
							hasMonth = true;
						}
						if(value.equals("") && lines[k][colRules].equals("_YY_")){
							hasYear = true;
						}
					}
					if(timex.contains("day")){
						hasDay = true;
					}
					if(timex.contains("week")){
						hasWeek = true;
					}
					if(timex.contains("month")){
						hasMonth = true;
					}
					if(timex.contains("year")){
						hasYear = true;
					}
					
					if(lines[i][colTimex].equals("B-DATE") || lines[i][colTimex].equals("B-TIME")){
						if(hasDay){
							value = "XXXX-XX-XX";
						}
						else if(hasWeek){
							value = "XXXX-WX";
						}
						else if(hasMonth){
							value = "XXXX-XX";
						}
						else if(hasYear){
							value = "XXXX";
						}
					}
					else{
						if(hasDay){
							value = "PXD";
						}
						else if(hasWeek){
							value = "PXW";
						}
						else if(hasMonth){
							value = "PXM";
						}
						else if(hasYear){
							value = "PXY";
						}
					}
				}
				
				for (int k = i; k < i+numTok; k++){
					lines[k][colValTimex] = value;
				}
				
				
				String beginPointID = "-";
				String endPointID = "-";
				if(anchorTimeID.equals("")){
					anchorTimeID = "-";
				}
				if(hasBeginPoint && pointID[0] == null){
					beginPointID = "tmx0";
				}
				else if(pointID[0] != null){
					beginPointID = pointID[0];
				}
				if(hasEndPoint && pointID[1] == null){
					endPointID = "tmx0";
				}
				else if(pointID[1] != null){
					endPointID = pointID[1];
				}
				
				if (beginPointID.equals(endPointID)){
					beginPointID = "-";
					endPointID = "-";
				}
				
				for (int k = i; k < i+numTok; k++){
					lines[k][colAnchor] = anchorTimeID;
					lines[k][colBeginPoint] = beginPointID;
					lines[k][colEndPoint] = endPointID;
				}
				
				/*if(lines[i][colTimex].equals("B-DATE") && Integer.parseInt(lines[i][colSent])<3 && dctIsTodayDate
						&& value.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]")){
					dct = value;
					dctIsTodayDate = false;
				}*/
				
			}
		}
		}
		
		int cpt = 100;
		int i = lines.length-1;
		boolean found = false;
		while(i > 0 && !found){
			if(lines[i] != null && lines[i].length > 3 && lines[i][colTimexID] != null && lines[i][colTimexID].startsWith("tmx")){
				cpt = Integer.parseInt(lines[i][colTimexID].replace("tmx", ""));
				found = true;
			}
			i--;
		}
		
		cpt ++;
		//int cpt = Integer.parseInt(idTimex.substring(3));
		lines = getEmptyTimex(lines, dct, cpt);
		
		return lines;
	}
	
	
	/**
	 * 
	 */
	public static boolean checkTimex (String timex){
		//boolean timexValid = true;
		
		if (timex.matches("^[0-9]([0-9])?[-.][0-9][0-9] .*$")){
			return false;
		}
		
		return true;
	}
	
	/**
	 * Modify the timex in order it can be parsed by TimeNorm
	 * @param timex
	 * @param lines_timex
	 * @return
	 */
	public static String replaceFailValue(String timex, String[][] lines_timex){
		String newTimex = "";
		if (timex.contains(" to ") || timex.contains(" of ")){
			Pattern p = Pattern.compile(" (to|of) (.*)$");
			Matcher m = p.matcher(timex);
			if(m.find()){
				newTimex = m.group(2);
			}
		}
		else if (timex.contains(" or ") || timex.contains(" and ")){
			int cc = 0;
			Boolean unit = false;
			for(int k=0; k<lines_timex.length; k++){
				if(cc>0 && !unit){
					newTimex += lines_timex[k][colToken]+" ";
				}
				if(lines_timex[k]!= null && lines_timex[k][colLemma] != null 
						&& (lines_timex[k][colLemma].equals("or") || lines_timex[k][colLemma].equals("and"))){
					cc = k;
					if(unit){
						for(int l=0; l<cc; l++){
							newTimex += lines_timex[l][colToken]+" ";
						}
					}
				}
				if(cc==0 && lines_timex[k] != null && lines_timex[k][colRules] != null && lines_timex[k][colRules].equals("_UNIT_")){
					unit = true;
				}
			}
		}
		else if(timex.contains("at least")){
			Pattern p = Pattern.compile("(at least) (.*)$");
			Matcher m = p.matcher(timex);
			if(m.find()){
				newTimex = m.group(2);
			}
		}
		else if(timex.contains("future")){
			newTimex = "future";
		}
		else if(timex.contains("coming")){
			Pattern p = Pattern.compile("^(.*coming) (.*)$");
			Matcher m = p.matcher(timex);
			if(m.find()){
				newTimex = m.group(2);
			}
		}
		else if(timex.contains("earlier")){
			Pattern p = Pattern.compile("(.*earlier) (.*)$");
			Matcher m = p.matcher(timex);
			if(m.find()){
				newTimex = m.group(2);
			}
		}
		else if(timex.contains("term")){
			Pattern p = Pattern.compile("(.*) term$");
			Matcher m = p.matcher(timex);
			if(m.find()){
				newTimex = m.group(1);
			}
		}
		
		return newTimex;
	}
	
	
	/**
	 * Modify the value in two cases:
	 * - when the value format is not compatible with the type of the timex
	 * - when an new anchorTime is needed (in case of future event for example)
	 * @param timex
	 * @param anchorTime
	 * @param value
	 * @param lines_timex
	 * @param typeModif
	 * @return
	 * @throws ParseException
	 */
	public static String compute_value(String timex, String anchorTime, String value, String[][] lines_timex, String typeModif) throws ParseException{
		
		String year = anchorTime.substring(0,4);
		String month = anchorTime.substring(5,7);
		String day = anchorTime.substring(8,10);
		
		
		if(typeModif.equals("quarter")){
			int j = 0;
			Boolean find = false;
			Boolean nextTimex = false;
			String quarter_month = "";
			String quarter = "";
			while(lines_timex[j] != null && find == false){
				if(lines_timex[j][colTimex] != null && lines_timex[j][colTimex].startsWith("B-") && j>0){
					nextTimex = true;
				}
				if(nextTimex){
					if(lines_timex[j][colRules] != null && lines_timex[j][colRules].equals("_MONTH_")){
						find = true;
						quarter_month = lines_timex[j][colLemma];
					}
				}
				if(nextTimex && lines_timex[j][colTimex] != null && lines_timex[j][colTimex].equals("O")){
					nextTimex = false;
					find = true;
				}
				j++;
			}
			if(quarter_month.matches("mar.*")){
				quarter = "Q1";
			}
			if(quarter_month.matches("jun.*")){
				quarter = "Q2";
			}
			if(quarter_month.matches("sept.*")){
				quarter = "Q3";
			}
			if(quarter_month.matches("dec.*")){
				quarter = "Q4";
			}
			
			if(!quarter.equals("")){
				Pattern p = Pattern.compile("Q(X|1|2|3|4)");
				value = p.matcher(value).replaceFirst(quarter);
			}
		}
		
		if(typeModif.equals("in") || typeModif.equals("late") || typeModif.equals("early")){
			
			int number = 0;
			for (int i=0; i<lines_timex.length; i++){
				if(lines_timex[i] != null && lines_timex[i][colRules].equals("_CN_") 
						&& numbers.containsKey(lines_timex[i][colLemma])){
					number = Integer.parseInt(numbers.get(lines_timex[i][colLemma]));
				}
			}
			if(number == 0 && typeModif.equals("late")){
				number = 1;
			}
			else if(number == 0 && typeModif.equals("early")){
				number = -1;
			}
		
			if(number != 0){
				if(timex.contains("year")){
					String newYear = Integer.toString(Integer.parseInt(year) + number);
					value = newYear;
				}
				else if(timex.contains("day")){
					Date date1 = new Date();
					Date dateDCT = new Date();
					
					dateDCT = new SimpleDateFormat(getFormat(anchorTime)).parse(anchorTime);
					value = addDays(dateDCT, number);
					//value = date1.toString();
				}
				else if(timex.contains("month")){
					if (Integer.parseInt(month) + number > 12){
						int addyear = (Integer.parseInt(month) + number)/12;
						year = Integer.toString(Integer.parseInt(year)+addyear);
						month = Integer.toString(Integer.parseInt(month) + ((Integer.parseInt(month) + number) - (12 * addyear)));
						value = year+"-"+month+"-"+day;
					}
				}
			}
		}
		else if(typeModif.equals("bef/af")){
			if (timex.contains("day")){
				value = "XXXX-XX-XX";
			}
			else if(timex.contains("weeks")){
				value = "XXXX-XX";
			}
			else if(timex.contains("week")){
				value = "XXXX-WXX";
			}
		}
		else if(typeModif.equals("addWeek")){
			Date date = new Date();
			date = new SimpleDateFormat(getFormat(value)).parse(value);
			value = addDays(date, 7);
		}
		else if(typeModif.equals("addYear")){
			value = Integer.toString(Integer.parseInt(year)+1)+"-"+month+"-"+day;
		}
		
		return value;
	}
	
	
	/**
	 * Add days to a date
	 * @param date
	 * @param days
	 * @return
	 * @throws ParseException
	 */
	public static String addDays(Date date, int days) throws ParseException
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        Date date1 = new Date();
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
        
        String valueDate = format1.format(cal.getTime());
        return valueDate;
    }
	
	
	/**
	 * Check if a date is valid (for example if date=2012-02-31 modify in date=2012-03-03
	 * @param date
	 * @return
	 */
	public static String checkDate (String date){
		if(date.length() == 10){
			String year = date.substring(0,4);
			String month = date.substring(5,7);
			String day = date.substring(8,10);
			if(month.equals("02") && (day.equals("30") || day.equals("31") || day.equals("29"))){
				month = "03";
				day = "0"+Integer.toString(Integer.parseInt(day) - 28);
			}
			if((month.equals("04") || month.equals("06") || month.equals("09") || month.equals("11")) && day.equals("31")){
				day = "01";
				month = Integer.toString(Integer.parseInt(month)+1);
				if(month.length() == 1){
					month = "0"+month;
				}
				if(month.equals("13")){
					month = "01";
					year = Integer.toString(Integer.parseInt(year)+1);
				}
			}
			date = year+"-"+month+"-"+day;
		}
		return date;
	}
	
	/**
	 * Get the value of
			} a timex from its id
	 * If no value has been yet computed, send the timex to TimeNorm
	 * @param id
	 * @param lines
	 * @param anchorTime
	 * @return
	 */
	public static String getValTimexID(String id, String [][] lines, String anchorTime){
		String val = "";
		int k = 0;
		while (val.equals("") && k<lines.length){
			if(lines[k]!= null && lines[k][colTimexID] != null && !lines[k][colTimexID].equals("_NULL_") && lines[k][colTimexID].equals(id)){
				if(lines[k][colValTimex] != null && lines[k][colValTimex] != ""){
					val = lines[k][colValTimex];
				}
				else{
					String timex = lines[k][colToken];
					int j = k+1;
					while(j<lines.length && lines[j][colTimex].startsWith("I-")){
						timex += " "+lines[j][colToken];
						j++;
					}
					String listValuesString = "";
					long start = System.currentTimeMillis();
					long end = start + 120; // 60 seconds * 1000 ms/sec
					while (System.currentTimeMillis() < end)
					{
						listValuesString = TestTimeNorm_parseAll.getTimeNorm (timex, anchorTime);
					}
					val = listValuesString.split(";")[0];
				}
			}
			k++;
		}
		
		if(val.length() == 4){
			val += "-01-01";
		}
		else if(val.length() == 7){
			val += "-01";
		}
		if(val.contains("-Q")){
			if(val.contains("Q1")){
				val = val.replace("Q1", "01");
			}
			else if(val.contains("Q2")){
				val = val.replace("Q2", "04");
			}
			else if(val.contains("Q3")){
				val = val.replace("Q3", "07");
			}
			else if(val.contains("Q4")){
				val = val.replace("Q4", "10");
			}
		}
		
		if(!val.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]")){
			val = anchorTime;
		}
		
		return val;
	}
	
	/**
	 * Search the beginPoint and endPoint of a duration
	 * @param timex
	 * @param anchorTime
	 * @param lines
	 * @param sentence
	 * @param indStartTimex
	 * @param indEndTimex
	 * @return an array containing two values: the beginPoint id and the endPoint id
	 */
	public static String[] getBegEndPoint (String timex, String anchorTime, String[][]lines, 
			int [] sentence, int indStartTimex, int indEndTimex){
		String point [] = new String [2];
		
		//recent days
		if(timex.contains("last") || timex.contains("recent") || timex.contains("next")){
			point[1] = "tmx0";
		}
		// first year after
		else if(timex.contains("first")){
			if (lines[indEndTimex] != null && lines[indEndTimex][colLemma] != null 
					&& lines[indEndTimex][colLemma].equals("of") && indEndTimex<lines.length-1
					&& lines[indEndTimex+1] != null && lines[indEndTimex+1][colTimex].equals("B-DATE")){
				point[0] = lines[indEndTimex][colTimexID];
			}
			else{
				point[0] = "tmx0";
			}
		}
		// the year ending DATE
		else if(lines[indEndTimex] != null && lines[indEndTimex][colLemma] != null 
				&& lines[indEndTimex][colLemma].equals("end") && indEndTimex<lines.length-1
				&& lines[indEndTimex+1] != null && lines[indEndTimex+1][colTimex] != null
				&& lines[indEndTimex+1][colTimex].equals("B-DATE")){
			point[1] = lines[indEndTimex+1][colTimexID];
		}
		//DATE, nearly two weeks after ...
		else if (lines[indEndTimex] != null && lines[indEndTimex][colLemma] != null 
				&& lines[indEndTimex][colLemma].equals("after")){
			for(int k=sentence[0]; k<indStartTimex; k++){
				if (lines[k] != null && lines[k][colTimex] != null && lines[k][colTimex].equals("B-DATE")){
					point[1] = lines[k][colTimexID];
				}
			}
		}
		// after two weeks
		else if (indStartTimex > 0 && lines[indStartTimex-1] != null && lines[indStartTimex-1][colLemma] != null 
				&& lines[indStartTimex-1][colLemma].equals("after")){
			if(sentence[0] < indStartTimex){
				for(int k=sentence[0]; k<indStartTimex; k++){
					if (lines[k] != null && lines[k][colTimex] != null && lines[k][colTimex].equals("B-DATE")){
						point[0] = lines[k][colTimexID];
					}
				}
			}
		}
		// two weeks, starting at DATE
		else{
			for (int k = indEndTimex; k<sentence[1]; k++){
				if (lines[k] != null && lines[k][colLemma] != null && lines[k][colLemma].equals("start")){
					for (int l=k+1; l<k+4 && l<sentence[1]; l++){
						if (lines[l] != null && lines[l][colTimex] != null && lines[l][colTimex].equals("B-DATE")){
							point[0] = lines[l][colTimexID];
							break;
						}
					}
					break;
				}
			}
		}
		
		return point;
	}
	
	/**
	 * Given a timex, search its anchorTime
	 * @param timex
	 * @param anchorTime
	 * @param lines
	 * @param sentence
	 * @param indStartTimex
	 * @param indEndTimex
	 * @return anchorTime id
	 */
	public static String getAnchorTime (String timex, String anchorTime, String[][]lines, 
			int [] sentence, int indStartTimex, int indEndTimex){
		String newAnchorTime = "";
		if(timex.contains("earlier") || timex.contains("time") || timex.contains("following") 
				|| isTimexInQuote(indStartTimex, lines) 
				|| (lines[indStartTimex][colTimex].equals("B-TIME") && ! timex.contains("day") & ! timex.contains("this"))){
			//search a date in the sentence
			for(int k=sentence[0]; k<indStartTimex; k++){
				if(lines[k] != null && lines[k][colTimex] != null &&  lines[k][colTimex].equals("B-DATE")){
					newAnchorTime = lines[k][colTimexID];
				}
			}
			//search in the previous sentence
			if(newAnchorTime.equals("")){
				int j = sentence[0]-2;
				
				while(j>0 && lines[j][colToken] != null){
					if(lines[j] != null && lines[j][colTimex] != null && lines[j][colTimex].equals("B-DATE")){
						newAnchorTime = lines[j][colTimexID];
					}
					j--;
				}
			}
		}
		else if(lines[indEndTimex] != null && lines[indEndTimex][colLemma] != null 
				&& lines[indEndTimex][colLemma].equals("end") && indEndTimex<lines.length-1
				&& lines[indEndTimex+1] != null && lines[indEndTimex+1][colTimex] != null 
				&& lines[indEndTimex+1][colTimex].equals("B-DATE")){
			newAnchorTime = lines[indEndTimex+1][colTimexID];
		}
		return newAnchorTime;
	}
	
	/**
	 * test if a timex is into quotes
	 * @param ind
	 * @param lines
	 * @return
	 */
	public static boolean isTimexInQuote (int ind, String[][]lines){
		boolean inQuote = false;
		for(int k=0; k<ind; k++){
			if (lines[k] != null && lines[k][colToken].equals("''")){
				inQuote = false;
			}
			if (lines[k] != null && lines[k][colToken].equals("``")){
				inQuote = true;
			}
		}
		return inQuote;
	}
	
	/**
	 * From a dateString, get its format
	 * @param date
	 * @return
	 */
	public static String getFormat (String date){
		String format = "";
		if(date.matches("[0-9]+-[0-9]+-[0-9]+")){
			format = "yyyy-MM-dd";
		}
		else if(date.matches("[0-9]+-[0-9]+")){
			format = "yyyy-MM";
		}
		else if(date.matches("[0-9]+")){
			format = "yyyy";
		}
		return format;
	}
	
	/**
	 * Add a timex id to all the timexes
	 * @param lines
	 * @return
	 */
	public static String[][] addTimexID(String [][] lines){
		int cpt = 0;
		for (int i=0; i<lines.length; i++){
			if(lines[i] != null && lines[i][colTimex] != null && lines[i][colTimex].matches("(B|I)-(DATE|SET|TIME|DURATION)")){
				if(lines[i][colTimex].startsWith("B-")){
					cpt++;
				}
				lines[i][colTimexID] = "tmx"+cpt;
			}
			else{
				lines[i][colTimexID] = "_NULL_";
			}
		}
		return lines;
	}

	/**
	 * Concatenation of two arrays at two dimensions
	 * @param s
	 * @param e
	 * @param nbCol
	 * @return
	 */
	public static String [][] concatArray (String [][] s, String [][] e, int nbCol){
		String [][] newArray = new String [s.length+e.length][nbCol];
		int j = 0;
		for (int k=0; k<s.length; k++){
			newArray[j] = s[k];
			j++;
		}
		for (int k=0; k<e.length; k++){
			newArray[j] = e[k];
			j++;
		}
		
		return newArray;
	}
	
	/**
	 * Get the current date
	 * @return
	 */
	public static String getTodayDate (){
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String dateString = dateFormat.format(date).toString();
		return dateString;
	}
	
	/**
	 * get dct in the first 2 sentences
	 */
	public static String[] getDateFirstSentences(String[][] lines){
		String dct = "";
		String ind = "-1";
		int j=0;
		int sent = 0;
		boolean find = false;
		while(sent < 4 && !find){
			if (lines[j] != null && lines[j][colTimex] != null && lines[j][colTimex].equals("B-DATE")){
				String timex = lines[j][0];
				if(lines[j+1] != null && lines[j+1][colTimex] != null && lines[j+1][colTimex].equals("I-DATE")){
					int k = j+1;
					while(lines[k] != null && lines[k][colTimex] != null && lines[k][colTimex].equals("I-DATE")){
						timex += " "+lines[k][0];
						k++;
					}
				}
				if(timex.matches(".*[0-9][0-9][0-9][0-9].*")){
					
					dct = TestTimeNorm.getTimeNorm(timex, getTodayDate());
					if(dct.matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]")){
						find = true;	
					}
				}
			}
			if((lines[j] == null || lines[j][1] == null) && !lines[j][0].startsWith("# ") ){
				sent ++;
			}
			j++;
		}
		j=0;
		sent=0;
		if(!find){
			while(sent < 4 && !find){
				if(lines[j] != null && lines[j][0].matches("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]")){
					long start = System.currentTimeMillis();
					long end = start + 120; // 60 seconds * 1000 ms/sec
					while (System.currentTimeMillis() < end)
					{
						dct = TestTimeNorm.getTimeNorm(lines[j][0], getTodayDate());
					}
					ind = Integer.toString(j);
					find = true;
				}
				if((lines[j] == null || lines[j][1] == null)  && !lines[j][0].startsWith("# ") ){
					sent ++;
				}
				j++;
			}
		}
		String [] returnValue = new String [2];
		returnValue[0] = dct;
		returnValue[1] = ind;
		
		return returnValue;
	}
	
	
	private static String[][] getEmptyTimex (String [][] lines, String dct, int cptTimex) throws ParseException{
		List<String[]> emptyTx = new ArrayList<String[]> ();
		
		for(int i=0; i<lines.length; i++){
			//int indLastTok = -1;
	
			// if current token is B-TIMEX
			if (lines[i] != null && lines[i][colTimex] != null && lines[i][colTimex].startsWith("B-")
					&& !lines[i][colValTimex].equals("FAIL") && lines[i][colValTimex] != null){
				String timex = lines[i][colToken];
				
				//get the last index of the TIMEX token (in case of multi-tokens timex)
				for (int j=i+1; j<lines.length; j++){
					if(lines[j][colTimex] != null && lines[j][colTimex].startsWith("I-")){
						timex += " "+lines[j][colToken];
						//indLastTok = j;
					}
					else{
						break;
					}
				}
				
				if(lines[i][colTimex].endsWith("TIME") || lines[i][colTimex].endsWith("DATE")){
					boolean between = false;
					boolean and = false;
					boolean from = false;
					boolean to = false;
					boolean dash = false;
					boolean endFirstTimex = false;
					int secondTimex = -1;
					
					
					// token-1 == dal ("dalle 7 alle 10")
					if(i>1 && lines[i-1][colToken].toLowerCase().startsWith("from")){
						from = true;
					}
					if(i>1 && lines[i-1][colToken].toLowerCase().startsWith("between")){
						between = true;
					}
					for(int j=i+1; j<lines.length; j++){
						// if between two timexes there are "al" or "-" or "/" ("dalle 7 alle 10")
						if(lines[j][colTimex] != null && !lines[j][colTimex].equals("I-TIME") 
								&& !lines[j][colTimex].equals("I-DATE") 
								&& !endFirstTimex){
							endFirstTimex = true;
							if(lines[j][colToken].toLowerCase().startsWith("to")){
								to = true;
							}
							if(lines[j][colToken].toLowerCase().startsWith("and")){
								and = true;
							}
							if(lines[j][colToken].equals("-") || lines[j][colToken].equals("/")){
								dash = true;
							}
						}
						else if(endFirstTimex && lines[j][colTimex] != null 
								&& (lines[j][colTimex].endsWith("TIME") || lines[j][colTimex].endsWith("DATE"))
								&& !lines[j][colValTimex].equals("FAIL")){
							secondTimex = j;
							break;
						}
						else if(lines[j][colTimex] == null || endFirstTimex){
							break;
						}
						
					}
					// if there are two timex separated by "al" or "-"
					if((from && to && secondTimex > -1 || dash && secondTimex > -1 || between && and && secondTimex > -1 )
							&& lines[i][colValTimex].matches("[0-9]+.*") && lines[secondTimex][colValTimex].matches("[0-9]+.*")){
					
						// build an empty timex of type DURATION
						String [] eTx = new String [nbCol];
						eTx[colTimex] = "B-DURATION";
						eTx[colTimexID] = "tmx"+Integer.toString(cptTimex++);
						
						SimpleDateFormat format = null;
						String formatString = getFormat(lines[i][colValTimex]);
						if(!formatString.equals("")){
							format = new SimpleDateFormat(formatString);
						}
						
						
						if(format != null){
							
							//System.out.println(lines[i][colValTimex]);
							Date d1 = format.parse(lines[i][colValTimex]);
							
							SimpleDateFormat formatSecondTmx = null;
							String formatSecondTmxString = getFormat(lines[secondTimex][colValTimex]);
							if(!formatSecondTmxString.equals("")){
								formatSecondTmx = new SimpleDateFormat(formatSecondTmxString);
							}
							
							Date d2 = formatSecondTmx.parse(lines[secondTimex][colValTimex]);
							
							long diff = d2.getTime() - d1.getTime();
	
							long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
							long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
							long hours = TimeUnit.MILLISECONDS.toHours(diff);
							long day = TimeUnit.MILLISECONDS.toDays(diff);
							String value = "";
							
							if(day > 0){
								value = "P";
								if(day > 30 && day < 365 && day%30 < 2){
									value += Long.toString(day/30)+"M";
								}
								else if(day > 30 && day < 365){
									value += Long.toString(day)+"D";
								}
								else if(day >= 365){
									value += Long.toString(day/365)+"Y";
								}
								else{
									value += Long.toString(day)+"D";
								}
							}
							else{
								value = "PT";
								if(hours > 0){
									value += Long.toString(hours)+"H";
									if(minutes > hours*60){
										value += Long.toString(minutes - hours*60)+"M";
									}
								}
								else if(minutes > 0){
									value += Long.toString(minutes)+"M";
								}
							}
							
							eTx[colValTimex] = value;
							//eTx[0] = eTx[colValTimex];
							eTx[0] = "ETX";
							eTx[colBeginPoint] = lines[i][colTimexID];
							eTx[colEndPoint] = lines[secondTimex][colTimexID];
							emptyTx.add(eTx);
						}
					}
				}
				// "due settimane fa" or "da 3 mesi" or "dopo 2 mesi"
				else if((timex.contains(" ago") || (i>1 && lines[i-1][colToken].toLowerCase().equals("since") || lines[i-1][colToken].toLowerCase().equals("from"))
						|| (i>1 && lines[i-1][colToken].toLowerCase().startsWith("in") && !timex.contains("first") && timex.contains(" "))
						|| (i>1 && lines[i-1][colToken].toLowerCase().startsWith("after"))
						|| timex.contains(" after")) 
						&& lines[i][colTimex].endsWith("DURATION")){
					
					String [] eTx = new String [nbCol];
					if(lines[i][colValTimex].startsWith("P")){
						if(!timex.contains(" ago")){
							timex = timex+" ago";
						}
						
						String value = ""; 
						long start = System.currentTimeMillis();
						long end = start + 120; // 60 seconds * 1000 ms/sec
						while (System.currentTimeMillis() < end)
						{
							value = TestTimeNorm_parseAll.getTimeNorm(timex.toLowerCase(), dct);
						}
						if(value.equals("")){
							value = "FAIL;";
						}
						
						String [] listVal = value.split(";");
						for(int l=0; l<listVal.length; l++){
							if(!listVal[l].startsWith("P")){
								value = listVal[l];
								break;
							}
						}
						
						// if timeNorm failed, some simple rules to get the value
						if(value.startsWith("P") || value.equals("FAIL")){
							if (timex.contains("last")){
								value = "PAST_REF";
							}
							else if(timex.contains("next")){
								value = "FUTURE_REF";
							}
							else if(timex.contains("previous")){
								value = "PAST_REF";
							}
							else if(i>1 && lines[i-1][colToken].toLowerCase().equals("after")){
								value = "FUTURE_REF";
							}
							else if(timex.contains("after")){
								value = "FUTURE_REF";
							}
							else{
								value = "PAST_REF";
							}
						}
						else{
							if(timex.contains("year") && value.matches("[0-9]+-[0-9]+-[0-9].*")){
								value = value.substring(0,4);
							}
						}
						//eTx[0] = value;
						eTx[0] = "ETX";
						eTx[colValTimex] = value;
						eTx[colTimex] = "B-DATE";
						eTx[colTimexID] = "tmx"+Integer.toString((cptTimex++));
					}
					else if(lines[i][colValTimex].matches("[0-9]+.*")){
						//eTx[0] = lines[i][colValTimex];
						eTx[0] = "ETX";
						eTx[colValTimex] = lines[i][colValTimex];
						eTx[colTimex] = "B-DATE";
						eTx[colTimexID] = "tmx"+Integer.toString((cptTimex++));
						timex = timex.replace(" ago","");
						timex = "for "+timex;
						
						String value = TestTimeNorm.getTimeNorm(timex.toLowerCase(), dct);
						for (int j=i; j<lines.length; j++){
							if(lines[j][colTimex].equals("I-DURATION")){
								lines[j][colValTimex] = value;
							}
							else{
								break;
							}
						}
					}
					eTx[colAnchor] = lines[i][colTimexID];
					emptyTx.add(eTx);
				}
			}
		}
		
		for(int e = 0; e<emptyTx.size(); e++){
			String eTx = "\n";
			for (int l=0; l<emptyTx.get(e).length; l++){
				eTx += emptyTx.get(e)[l]+"\t";
			}
			lines[lines.length-1][0] += eTx+"\n";
		}
		
		return lines;
	}
	
	
	
	/**
	 * @param args
	 * @throws ParseException 
	 * @throws FileNotFoundException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws ParseException, FileNotFoundException, InterruptedException {
		// TODO Auto-generated method stub
		fill_hash_numbers();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			
		String repOutFolder = args[0];
		String [][] lines = TextProFileFormat.readFileTextPro(br, nbCol, false);
			
		String dct = TextProFileFormat.getDateFromLine(lines[1][0]);
		if (dct.matches("^\\d\\d\\d\\d.\\d\\d.\\d\\d.*$")) {
			dct = dct.substring(0,4)+"-"+dct.substring(5,7)+"-"+dct.substring(8,10);
	    } else if (dct.matches("^\\d\\d\\d\\d\\d\\d\\d\\d.*$")) {
	    	dct = dct.substring(0,4)+"-"+dct.substring(4,6)+"-"+dct.substring(6,8);
	    } else if (dct.matches("^\\d\\d\\d\\d-\\d\\d-\\d\\d.*$")) {
	    	dct = dct.substring(0,4)+"-"+dct.substring(4,6)+"-"+dct.substring(6,8);
	    } else{
	    	String dctInd [] = getDateFirstSentences(lines);
	    	dct = dctInd[0];
	    	if(!dctInd[1].equals("-1")){
	    		int ind = Integer.parseInt(dctInd[1]);
	    		lines[ind][colTimex] = "B-DATE";
	    	}
	    	
	    	if(dct.equals("") || !dct.matches("^\\d\\d\\d\\d-\\d\\d-\\d\\d.*")){
	    		dct = getTodayDate();
	    		dctIsTodayDate = true;
	    	}
		}
			
		String header [][] = Arrays.copyOfRange(lines,0,3);
		if(header[1][0].matches("^# DATE: $")){
			header[1][0] = "# DATE: "+dct;
		}
		else if(header[1][0].startsWith("# DATE") && !header[1][0].matches(".*T[0-9][0-9]:.*")){
			header [1][0] = "# DATE: "+dct;
		}
		
		lines = addTimexID(Arrays.copyOfRange(lines, 4, lines.length));
		lines = addTimexValue(lines,dct);
		TextProFileFormat.writeTextProFile(concatArray(header,lines,nbCol), repOutFolder, nbCol);
	}

}
