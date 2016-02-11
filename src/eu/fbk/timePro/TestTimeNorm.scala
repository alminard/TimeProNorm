package eu.fbk.timePro

import info.bethard.timenorm.TemporalExpressionParser
import info.bethard.timenorm.Temporal
import info.bethard.timenorm.TimeSpan
import info.bethard.timenorm._
import scala.util._
import java.io._
import scala.io.Source
import scala.collection.mutable.ListBuffer

object TestTimeNorm {
  
  def getTimeNorm (timex:String, anchorTime:String) : String = {
	//create a new parser (using the default English grammar)
	val parser = new TemporalExpressionParser()
	
	if (anchorTime.length() < 10 
	    || !anchorTime.matches("^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9].*")){
		return "FAIL"
	}
	else{
		//establish an anchor time
		val yearAnchor = Integer.parseInt(anchorTime.substring(0,4))
		val monthAnchor = Integer.parseInt(anchorTime.substring(5,7))
		val dayAnchor = Integer.parseInt(anchorTime.substring(8,10))
		
		val anchor = TimeSpan.of(yearAnchor, monthAnchor, dayAnchor)
		//parse an expression given an anchor time (here, assuming it succeeds)
		
		parser.parse(timex, anchor) match {
	        case Failure(exception) =>
	          return "FAIL"
	        case Success(temporal) =>
	          return temporal.timeMLValue
	      }
	}
  }

}