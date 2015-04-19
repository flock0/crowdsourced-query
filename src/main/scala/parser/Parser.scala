package parser
import _
import scala.util.parsing.combinator.RegexParsers
//SELECT (movies) FROM « Movies with Angelina Jolie » JOIN SELECT (movies) FROM « Movies with Brad Pitt » ON movies
//
//SELECT (full name, NUMERIC age of death) FROM « Presidents of the USA » WHERE « political party is democrat » ORDER BY age of death
object Parser extends RegexParsers with java.io.Serializable{
  
	def parseQ: Parser[Q] = (
	  "(" ~ parseQ ~ ") JOIN (" ~ parseQ ~ ") ON " ~ parseE ^^ {case _ ~ left ~ _ ~ right ~ _ ~ on => Join(left, right, on)}
	  | "(" ~ parseQ ~ ") IN (" ~ parseQ ~ ")" ^^ {case _ ~ left ~ _ ~ right ~ _ => In(left, right)}
	  | "(" ~ parseQ ~ ") NOT IN (" ~ parseQ ~ ")" ^^ {case _ ~ left ~ _ ~ right ~ _ => NotIn(left, right)}
	  | "(" ~ parseQ ~ ") INTERSECT (" ~ parseQ ~ ")" ^^ {case _ ~ left ~ _ ~ right ~ _ => Intersect(left, right)}
	  | "(" ~ parseQ ~ ") UNION (" ~ parseQ ~ ")" ^^ {case _ ~ left ~ _ ~ right ~ _ => Union(left, right)}
	  | parseQ1 ^^ {case q1 => q1}
	)

	def parseQ1: Parser[Q1] = (
	  parseQ2 ~ "LIMIT" ~ parseI^^ {case q2 ~ _ ~ lim => Limit(q2, lim)}
	  | parseQ2 ^^ {case q2 => q2}
	)

	def parseQ2: Parser[Q2] = (
	  parseQ3~"ORDER BY"~parseOrder ^^ {case q~_~o => OrderBy(q, o)}
	  | parseQ3 ^^ {case q3 => q3}
	)
	
	def parseOrder: Parser[List[O]] = (
		parseBaseOrder ~ opt(","~parseOrder) ^^ {
			case p1 ~ Some(_~p2) => p1 :: p2
			case p ~ None => List(p)
		}
	)
	
	def parseBaseOrder: Parser[O] = (
		parseE~"Desc" ^^ {case e~_ => Desc(e)}
		| parseE~opt("Asc") ^^ {case e ~ _ => Asc(e)}
	)

	def parseQ3: Parser[Q3] = (
	  parseQ4 ~ "GROUP BY" ~ parseE ^^ {case q4~_~elem => Group(q4, elem)}
	  | parseQ4 ^^{case q4 => q4}
	)

	def parseQ4: Parser[Q4] = (
	  parseSelectTree ~ "WHERE" ~ parseC ^^ {case select~_~c => Where(select, c)}
	  | parseSelectTree ^^{case q5 => q5}
	)
	
	def parseSelectTree: Parser[SelectTree] = (
	  "SELECT (" ~ parseP ~ ") FROM" ~ parseQ ^^ {case _~p~_~e => Select(e, p)}
	  | parseNl ^^ {case nl5 => nl5}
	)
	
	def parseP: Parser[List[P]] = (
	  parseComplexP ~ opt(","~parseP) ^^ {
	    case p1 ~ Some(_~p2) => p1 :: p2
	    case p ~ None => List(p)
	  }
	)
	
	def parseComplexP: Parser[P] = (
		"SUM(" ~ parseBaseP ~ ")" ^^ {case _~p~_ => Sum(p)}
		| "DISTINCT(" ~ parseBaseP ~ ")" ^^ {case _~p~_ => Distinct(p)}
		| "COUNT(" ~ parseComplexP ~ ")" ^^ {case _~p~_ => Count(p)}
		| parseBaseP ^^ {case p => p}
	)

	def parseBaseP: Parser[BaseP] = (
	  "NUMERIC" ~ parseE ^^ {case _~p => ElementNum(p)}
	  | parseE ^^ {case p => ElementStr(p)}
	)

	def parseE: Parser[String] = (
	  elem ^^ {case e => e}
	)

	def parseNl: Parser[NaturalLanguage] = (
	  "["~nl~"]" ^^ {case _~nl~_ => NaturalLanguage(nl)}
	)

	def parseC: Parser[Condition] = (
	  parseE ~ "<" ~ parseI ^^ {case e ~_~ i => LessThan(e, i)}
	  | parseE ~ ">" ~ parseI ^^ {case e ~_~ i => GreaterThan(e, i)}
	  | parseE ~ "<=" ~ parseI ^^ {case e ~_~ i => LessThanOrEqual(e, i)}
	  | parseE ~ ">=" ~ parseI ^^ {case e ~_~ i => GreaterThanOrEqual(e, i)}
	  | parseE ~ "=" ~ parseT ^^ {case e ~_~ t => Equals(e, t)}
	  | parseE ~ "IN" ~ parseG ^^ {case e ~_~ g => ConditionIn(e, g)}
	  | parseE ~ "NOT IN" ~ parseG ^^ {case e ~_~ g => ConditionNotIn(e, g)}
	  | "(" ~ parseC ~ ") AND (" ~ parseC ~ ")" ^^ {case _~left ~_~ right~_ => And(left, right)}
	  | "(" ~ parseC ~ ") OR (" ~ parseC ~ ")" ^^ {case _~left ~_~ right~_  => Or(left, right)}
	  | parseNl ^^ {case nl4 => nl4}
	)

	def parseT: Parser[T] = (
		parseI ^^ {case i => i}
	 | parseB ^^ {case b => b}
	 | parseS ^^ {case s => s}
	 | parseQ ^^ {case q => q}
	)
	
	def parseB: Parser[B] = {
		"True" ^^ {case _ => True()}
		"False" ^^ {case _ => False()}
	}
	
	def parseS: Parser[S] = (
		"\"" ~ str ~ "\"" ^^ {case _~str~_ => StrL(str)}
		| parseNl ^^ {case nl3 => nl3}
	)
	
	def parseI: Parser[I] = (
	  int ^^ {case i => IntL(i.toInt)}
	  | parseNl ^^ {case nl2 => nl2}
	)

	def parseG: Parser[G] = (
		"("~parseBaseG~")" ^^ {case _~l~_ => CondGroup(l)}
		| parseNl ^^ {case nl1 => nl1}
		| parseQ ^^ {case q => q}
	)
	
	def parseBaseG: Parser[List[T]] = (
		parseT ~ opt(","~parseBaseG) ^^ {
			case p1 ~ Some(_~p2) => p1 :: p2
			case p ~ None => List(p)
		}
	)
	
	val elem: Parser[String] = "[A-Za-z0-9_ ]+".r
	val int: Parser[String] = "[0-9]+".r
	val nl: Parser[String] = "[a-zA-Z0-9 ]+".r
	val str: Parser[String] = "[A-Za-z0-9]".r
  
  def parseQuery(query: String): ParseResult[Q] = parse(parseQ, query) 
    
}
