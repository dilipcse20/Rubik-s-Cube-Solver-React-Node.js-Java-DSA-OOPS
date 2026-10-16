# Compiles the Java solver into the "out" folder.
# Run from the solver-java folder:   .\build.ps1
$files = Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName }
if (Test-Path out) { Remove-Item -Recurse -Force out }
New-Item -ItemType Directory out | Out-Null
javac -d out $files
if ($LASTEXITCODE -eq 0) { Write-Host "Java solver compiled into the 'out' folder." }
