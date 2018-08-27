on convertGraffleFile(canvasName, outputFile)
  tell application "OmniGraffle"
    export front document scope canvasName as "SVG" to file outputFile
    close front document
  end tell
end convertGraffleFile

on run
   convertGraffleFile("Flux", "Flux.svg")
end run
