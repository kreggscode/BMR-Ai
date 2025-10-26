Add-Type -AssemblyName System.Net.WebClient
$webClient = New-Object System.Net.WebClient
$webClient.DownloadFile("https://github.com/gradle/gradle/raw/v8.13.0/gradle/wrapper/gradle-wrapper.jar", "gradle\wrapper\gradle-wrapper.jar")
