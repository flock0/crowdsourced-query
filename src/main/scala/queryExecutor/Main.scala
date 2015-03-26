package queryExecutor

object Main extends App {
  
  val query = "SELECT (full name) FROM [Presidents of the USA]"// WHERE [political party is democrat] ORDER BY age of death"
  
  val queryExec = new QueryExecutor
  val parsedQuery = queryExec.parse(query)
}