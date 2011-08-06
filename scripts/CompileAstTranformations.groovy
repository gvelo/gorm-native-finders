includeTargets << grailsScript("_GrailsInit")

target(compileAstTranformations: "Compile AST tranformations") {
    
   	def destFile = "./lib/nativefinders-ast-transform.jar"
	def targetDir = "./target-ast"
	def srcDir = "./src/ASTTransformation"
		
	ant.delete(dir:targetDir, quiet:true)
	 
	ant.mkdir(dir:targetDir)	
	ant.mkdir(dir:"${targetDir}/META-INF")
	
	ant.copy( todir:targetDir , verbose:true ){
		fileset( dir:srcDir ){
			include( name:"META-INF/**")
		}
	}

	ant.javac( srcDir:srcDir , destDir:targetDir )
	
	ant.jar(destfile:destFile, basedir:targetDir, compress:true,index:true, level:9)
	
	ant.delete(dir:targetDir, quiet:true)

    
}

setDefaultTarget(compileAstTranformations)
