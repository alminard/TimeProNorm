/**
 * Empty Timex and beginPoint/endPoint for duration
 * Class used by TimePro
 * @author Anne-Lyse Minard
 * @version 1.0
 */

package eu.fbk.timepro;

import ixa.kaflib.Annotation;
import ixa.kaflib.KAFDocument;
import ixa.kaflib.KAFDocument.AnnotationType;
import ixa.kaflib.Predicate;
import ixa.kaflib.Timex3;
import ixa.kaflib.WF;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.JDOMException;

import eu.fbk.textProFormat.*;


public class TimeProEmptyTimex {

	static KAFDocument nafFile = null;
	static List<timexFeatStructure> listTxFeat = new ArrayList<timexFeatStructure> ();
	
	/**
	 * Build HashMap: key --> event id, value --> list (tense, aspect)
	 * @param lines
	 * @return
	 */
	private static HashMap<String,List<String>> getListIdEvTenseAspect(String[][] lines){
		HashMap<String,List<String>> listIdEvTA = new HashMap<String,List<String>> ();
		int colEvId = 10;
		int colTense = 13;
		for(int i=0; i<lines.length; i++){
			if(lines[i] != null && lines[i].length > 5 && lines[i][colEvId] != null && lines[i][colEvId].matches("^e[0-9]*")){
				List<String> listTA = new ArrayList<String> ();
				listTA.add(lines[i][colTense]);
				listTA.add(lines[i][colTense+1]);
				listIdEvTA.put(lines[i][colEvId], listTA);
			}
		}
		
		return listIdEvTA;
	}
	
	/**
	 * Build hashMap: key --> Word Form id, value --> timex
	 * @param listTx
	 * @return
	 */
	private static HashMap<String, Timex3> getTimeExsByWFs(List<Timex3> listTx){
		HashMap<String, Timex3> listTimexByWF = new HashMap<String, Timex3> ();
		for(Timex3 tx : listTx){
			if(tx.hasSpan() && tx.getSpan().size()>0){
				listTimexByWF.put(tx.getSpan().getTargets().get(0).getId(), tx);
			}
		}
		return listTimexByWF;
	}
	
	/**
	 * Build hashMap: key --> Word form id, value --> predicate
	 * @param listPred
	 * @return
	 */
	private static HashMap<String, Predicate> getPredByWFs(List<Predicate> listPred){
		HashMap<String, Predicate> listPredByWF = new HashMap<String, Predicate> ();
		for(Predicate pred : listPred){
			listPredByWF.put(pred.getSpan().getFirstTarget().getSpan().getFirstTarget().getId(), pred);
		}
		return listPredByWF;
	}
	
	/**
	 * get the position of a token in the list of word form
	 * @param listW
	 * @param tid
	 * @return
	 */
	private static int getPosList(List<WF> listW, String tid){
		int pos = -1;
		for(int j=0; j<listW.size(); j++){
			if(listW.get(j).getId().equals(tid)){
				pos = j;
			}
		}
		return pos;
	}
	
	/**
	 * Build hashMap: key --> timex id, value --> timex
	 * @param list_timex
	 * @return
	 */
	private static HashMap<String, Timex3> getTimexById (List<Timex3> list_timex){
		HashMap<String, Timex3> listTimexById = new HashMap<String,Timex3> ();
		for (Timex3 t : list_timex){
			listTimexById.put(t.getId(), t);
		}
		
		return listTimexById;
	}
	
	/**
	 * main method to identify the empty timex to represent in NAF
	 * @param listIdEvTenseAspect
	 * @throws ParseException
	 */
	public static void addEmptyTimex(HashMap<String,List<String>> listIdEvTenseAspect) throws ParseException{
		
		ListIterator<Timex3> timexl = nafFile.getTimeExs().listIterator();
		HashMap<String, Timex3> listTimexByWF = getTimeExsByWFs(nafFile.getTimeExs());
		HashMap<String, Predicate> listPredByWF = getPredByWFs(nafFile.getPredicates());
		
		HashMap<String, Timex3> listTimexById = getTimexById(nafFile.getTimeExs());
		
		String dct = "";
		
		//For each timex3 of type DURATION: search for beginPoint and endPoint
		while (timexl.hasNext()){
			Timex3 tx = timexl.next();
			if(tx.getType().equals("DATE") && tx.getId().equals("tmx0")){
				dct = tx.getValue().split("T")[0];
			}
			if(tx.getType().equals("DURATION") && tx.hasSpan()){
				if(tx.getValue().startsWith("P")){
					timexFeatStructure txf = new timexFeatStructure();
					txf.timexElt = tx;
					txf.value = tx.getValue();
					txf.period = txf.value.substring(1);
					
					int sent = tx.getSpan().getTargets().get(0).getSent();
					
					String tid = tx.getSpan().getTargets().get(0).getId();
					String lastid = tx.getSpan().getTargets().get(tx.getSpan().getTargets().size()-1).getId();
					
					List<Annotation> list_wf_annot = nafFile.getBySent(AnnotationType.WF, sent);
					
					List<WF> listWords = new ArrayList<WF> ();
					for (Annotation annot : list_wf_annot){
						listWords.add((WF)annot);
					}
					
					int posList = getPosList(listWords, tid);
					int posLastList = getPosList(listWords, lastid);
					
					for(int j=posList-1; j>0; j--){
						if(listWords.get(j).getForm().matches(",")){
							break;
						}
						if(j > posList - 6 && listWords.get(j).getForm().matches("^((in)|(over)|(within)|(for)|(after))$")){
							if(txf.signalBefore == null){
								txf.signalBefore = listWords.get(j).getForm();
							}
						}
						if(j > (posList - 10) && listPredByWF.containsKey(listWords.get(j).getId())){
							if(txf.eventBefore == null){
								String evid = listWords.get(j).getId().replace("w", "e");
								if(listIdEvTenseAspect.containsKey(evid) && !listIdEvTenseAspect.get(evid).get(0).equals("NONE")){
									txf.eventBefore = listPredByWF.get(listWords.get(j).getId()).getId();
									txf.eventTenseBefore = listIdEvTenseAspect.get(evid).get(0)+"#"+listIdEvTenseAspect.get(evid).get(1);
								}
							}
						}
					}
					for(int j=posLastList+1; j<listWords.size(); j++){
						if(listWords.get(j).getForm().matches(",")){
							break;
						}
						if(j<posLastList+6 && listWords.get(j).getForm().matches("^((of)|(for)|(after))$")){
							if(txf.signalAfter == null){
								txf.signalAfter = listWords.get(j).getForm();
							}
						}
						if(j<posLastList+6 && listTimexByWF.containsKey(listWords.get(j).getId()) 
								&& listTimexByWF.get(listWords.get(j).getId()).getType().equals("DATE")){
							if(txf.timexAfter == null){
								txf.timexAfter = listTimexByWF.get(listWords.get(j).getId());
							}
						}
						if(j<posLastList+10 && listPredByWF.containsKey(listWords.get(j).getId())){
							if(txf.eventAfter == null){
								if(listWords.get(j-1).getForm().equals("of")){
									txf.eventAfter = "of";
								}
								else{
									txf.eventAfter = listPredByWF.get(listWords.get(j).getId()).getId();
									String evid = listWords.get(j).getId().replace("t", "e");
									if(listIdEvTenseAspect.containsKey(evid)){
										txf.eventTenseAfter = listIdEvTenseAspect.get(evid).get(0)+"#"+listIdEvTenseAspect.get(evid).get(1);
									}
								}
							}
						}
						
					}
					for(int j=posList; j<posLastList+1; j++){
						if(listWords.get(j).getForm().matches("^((last)|(past)|(next)|(recent)|(ago)|(first))$")){
							txf.signalInTimex = listWords.get(j).getForm();
						}
					}
					listTxFeat.add(txf);
				}
				else{
					tx.setType("DATE");
				}
			}
		}
		
		int nbElt = listTxFeat.size();
		for(int j=0; j<nbElt; j++){
			timexFeatStructure txf = listTxFeat.get(j);
			
			//IN|OVER|FOR [last/past/recent VAL] -> sp: DCT-VAL, ep: DCT
			//eg: in the last 24 hours
			if(txf.signalBefore != null && txf.signalBefore.matches("^((in)|(over)|(for))$") 
					&& txf.signalInTimex != null && txf.signalInTimex.matches("^((last)|(past)|(recent))$")){
				txf.timexElt.setEndPoint(listTimexById.get("tmx0"));
				if(!txf.period.contains("X")){
					Timex3 bpIdTx = getTimex(computeDate(dct, txf.period, "-"));
					txf.timexElt.setBeginPoint(bpIdTx);
				}
			}
			//WITHIN [VAL] (! of EV) -> sp: DCT, ep: DCT+VAL
			//eg: within the next two weeks
			else if(txf.signalBefore != null && txf.signalBefore.equals("within") 
					&& (txf.signalAfter == null || !txf.signalAfter.equals("of"))){
				txf.timexElt.setBeginPoint(listTimexById.get("tmx0"));
				if(!txf.period.contains("X")){
					Timex3 epIdTx = getTimex(computeDate(dct, txf.period, "+"));
					txf.timexElt.setEndPoint(epIdTx);
				}
				
			}
			//WITHIN [VAL] of EV -> sp: TA_EV, ep: TA_EV+VAL
			//eg: within a week of his capture
			/*else if(txf.signalBefore != null && txf.signalBefore.equals("within") 
					&& txf.signalAfter != null && txf.signalAfter.equals("of")
					&& txf.eventAfter != null){
			}*/
			//OVER [next VAL] -> sp: DCT, ep: DCT+VAL
			//eg: 
			else if(txf.signalInTimex != null && txf.signalInTimex.equals("next") 
					&& txf.signalBefore != null && txf.signalBefore.equals("over")){
				txf.timexElt.setBeginPoint(listTimexById.get("tmx0"));
				if(!txf.period.contains("X")){
					Timex3 epIdTx = getTimex(computeDate(dct, txf.period, "+"));
					txf.timexElt.setEndPoint(epIdTx);
				}
			}
			//EV(pres) FOR [VAL] -> sp: DCT, ep: DCT+VAL
			//eg: that would permit ... for at least 20 more years
			else if(txf.eventTenseBefore != null && txf.eventTenseBefore.matches("^PRES.*") 
					&& txf.signalBefore != null && txf.signalBefore.equals("for")){
				txf.timexElt.setBeginPoint(listTimexById.get("tmx0"));
				if(!txf.period.contains("X")){
					Timex3 epIdTx = getTimex(computeDate(dct, txf.period, "+"));
					txf.timexElt.setEndPoint(epIdTx);
				}
			}
			//EV(past)? FOR [VAL] -> sp: DCT-VAL, ep: DCT
			//eg: where they lived for 2 1/2 years
			else if((txf.eventTenseBefore == null || txf.eventTenseBefore.matches("^PAST.*")) 
					&& txf.signalBefore != null && txf.signalBefore.equals("for")){
				txf.timexElt.setEndPoint(listTimexById.get("tmx0"));
				if(!txf.period.contains("X")){
					Timex3 bpIdTx = getTimex(computeDate(dct, txf.period, "-"));
					txf.timexElt.setBeginPoint(bpIdTx);
				}
			}
			//EV(pres) [VAL] AFTER EV(past) -> sp: DCT-VAL, ep: DCT
			else if(txf.eventTenseBefore != null && txf.eventTenseBefore.matches("^PRES.*")
					&& txf.signalAfter != null && txf.signalAfter.equals("after")
					&& txf.eventTenseAfter != null && txf.eventTenseAfter.matches("^PAST.*")){
				txf.timexElt.setEndPoint(listTimexById.get("tmx0"));
				if(!txf.period.contains("X")){
					Timex3 bpIdTx = getTimex(computeDate(dct, txf.period, "-"));
					txf.timexElt.setBeginPoint(bpIdTx);
				}
			}
			//EV(past) OVER [VAL] -> sp: DCT-VAL, ep: DCT
			else if(txf.eventTenseBefore != null && txf.eventTenseBefore.matches("^PAST.*")
					&& txf.signalBefore != null && txf.signalBefore.equals("over")){
				txf.timexElt.setEndPoint(listTimexById.get("tmx0"));
				if(!txf.period.contains("X")){
					Timex3 bpIdTx = getTimex(computeDate(dct, txf.period, "-"));
					txf.timexElt.setBeginPoint(bpIdTx);
				}
			}
			//FOR [first VAL] [DATE] -> sp: TA, ep: TA+VAL
			else if(txf.signalBefore != null && txf.signalBefore.equals("for")
					&& txf.signalInTimex != null && txf.signalInTimex.equals("first")
					&& txf.timexAfter != null){
				txf.timexElt.setBeginPoint(listTimexById.get("tmx0"));
				if(!txf.period.contains("X")){
					Timex3 epIdTx = getTimex(computeDate(txf.timexAfter.getValue(), txf.period, "+"));
					txf.timexElt.setEndPoint(epIdTx);
				}
			}
			//AFTER [VAL] EV(past) -> sp: DCT-VAL, ep: DCT
			else if(txf.signalBefore != null && txf.signalBefore.equals("after")
					&& txf.eventTenseBefore != null && txf.eventTenseBefore.matches("^PAST.*")){
				txf.timexElt.setEndPoint(listTimexById.get("tmx0"));
				if(!txf.period.contains("X")){
					Timex3 bpIdTx = getTimex(computeDate(dct, txf.period, "-"));
					txf.timexElt.setBeginPoint(bpIdTx);
				}
			}
		}
	}
	
	/**
	 * If a timex with the given value exists: return timex element
	 * Else: create a new timex of type DATE with as value the given value. 
	 * @param txValue
	 * @return
	 */
	private static Timex3 getTimex (String txValue){
		int maxId = 0;
		for(timexFeatStructure tx : listTxFeat){
			if(tx.value.equals(txValue)){
				return tx.timexElt;
			}
			if(Integer.parseInt(tx.timexElt.getId().substring(3)) > maxId){
				maxId = Integer.parseInt(tx.timexElt.getId().substring(3));
			}
		}
		Timex3 tx = nafFile.newTimex3("DATE");
		tx.setValue(txValue);
		timexFeatStructure txf = new timexFeatStructure();
		txf.value = txValue;
		txf.timexElt = tx;
		listTxFeat.add(txf);
		return tx;
	}
	
	/**
	 * Compute a new date given a date and a period to add or remove.
	 * @param date1
	 * @param period
	 * @param addRemove
	 * @return
	 * @throws ParseException
	 */
	private static String computeDate (String date1, String period, String addRemove) throws ParseException{
		GregorianCalendar calendar = new GregorianCalendar(); 
		String dateFormat = "yyyy-MM-dd";
		if(date1.matches("^[0-9]*$") && date1.length() >= 8){
			dateFormat = "yyyyMMdd";
		}
		else if(date1.contains("/") && date1.length() >= 10){
			dateFormat = "yyyy/MM/dd";
		}
			
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
	
		try{
			calendar.setTime(sdf.parse(date1));
		
			
			Pattern p = Pattern.compile("^([0-9]*)([^0-9])$");
			Matcher m = p.matcher(period);
			
			if(period.startsWith("T")){
				if(date1.contains("T") && date1.contains(":")){
					String dateTimeFormat = dateFormat+"THH:mm:ss";
					SimpleDateFormat sdftime = new SimpleDateFormat(dateTimeFormat);
					try{
						calendar.setTime(sdftime.parse(date1));
					} catch(Exception e){
						e.printStackTrace();
					}
					int num = Integer.parseInt(period.substring(1,2));
					String unit = period.substring(2);
					if(addRemove.equals("-")){num = -num;}
					
					if(unit.equals("H")){calendar.add(Calendar.HOUR, num);}
				}
			}
			else if(m.find()){
				int num = Integer.parseInt(m.group(1));
				String unit = m.group(2);
				int num2 = -1;
				String unit2 = "";
				
				if(unit.length() > 1){
					unit = period.substring(1,2);
					num2 = Integer.parseInt(period.substring(2,3));
					unit2 = period.substring(3);
				}
				
				if(addRemove.equals("-")){num = -num;}
			
				if(unit.equals("W")){calendar.add(Calendar.WEEK_OF_YEAR, num);}
				else if(unit.equals("Y")){calendar.add(Calendar.YEAR, num);}
				else if(unit.equals("D")){calendar.add(Calendar.DAY_OF_YEAR, num);}
				else if(unit.equals("M")){calendar.add(Calendar.MONTH, num);}
				
				if(!unit2.equals("")){
					if(addRemove.equals("-")){num2 = -num2;}
					
					if(unit2.equals("W")){calendar.add(Calendar.WEEK_OF_YEAR, num2);}
					else if(unit2.equals("Y")){calendar.add(Calendar.YEAR, num2);}
					else if(unit2.equals("D")){calendar.add(Calendar.DAY_OF_YEAR, num2);}
					else if(unit2.equals("M")){calendar.add(Calendar.MONTH, num2);}
				}
			}
		
		} catch (Exception e){
			e.printStackTrace();
		}
		
		Date ep = calendar.getTime();
		
		sdf = new SimpleDateFormat("yyyy-MM-dd");
		String epS = sdf.format(ep);
		
		
		return epS;
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws JDOMException 
	 */
	public static void main(String[] args) throws IOException, ParseException, JDOMException {
		// TODO Auto-generated method stub
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		nafFile = KAFDocument.createFromStream(br);
		
		String [][] lines = TextProFileFormat.readFileTextPro(args[0], 15, true);
				
		HashMap<String, List<String>> listIdEvTenseAspect = getListIdEvTenseAspect(lines);
				
		addEmptyTimex(listIdEvTenseAspect);
		
		new PrintStream(System.out, true, "UTF-8").println(nafFile.toString());
	}

}

class timexFeatStructure{
	String signalBefore;
	String signalAfter;
	String eventBefore;
	String eventAfter;
	String eventTenseBefore;
	String eventTenseAfter;
	String signalInTimex;
	String value;
	Timex3 timexAfter;
	String period;
	Timex3 timexElt;
}
