# SEARCH ENGINE

![alt text](https://img.shields.io/badge/java-spring-green)
![alt text](https://img.shields.io/badge/lucence-morphology-red)
![alt text](https://img.shields.io/badge/My-SQL-blue)
![alt text](https://img.shields.io/badge/html-css%20js%20jquery-8A2BE2)
![alt text](https://img.shields.io/badge/study-project-violet)
![alt text](https://i.imgur.com/olaNb2z.png)
This project is an implementation of the backend component for a local search system🔎.
The frontend component has been previously developed by other developers.
With this application, you will be able to search for information within websites that you load through a YAML file.

📦✨

By using this application, you can search for information across the websites you load into the system using a YAML
file.
The backend component handles the search functionality, allowing you to find relevant information efficiently.

## Used Technologies stack📋

### Frontend:

1. HTML
2. CSS
3. JS
4. JQUERY

### Backend:

1. JAVA
2. Spring boot
3. Spring MVC
4. Spring DATA JPA
5. MySQL
6. Log4j
7. Lombok
8. Snake Yaml
9. Lucence Morphology

## Functional and how does it work?

The application contains 3 main sections that you can switch between:

### Dashboard

![alt text](https://i.imgur.com/NmNE6Rn.png)
Dashboard is a **statistics** menu where you can see sites and their amount, the number of lemmas loaded into the
database and some other parameters

### Management

![alt text](https://i.imgur.com/vOxCnUV.png)
Management menu - the main menu for configuring the program. Here you can start and stop indexing. On this page you can
also reindex the page if changes have been made to it

### Search

![alt text](https://i.imgur.com/olaNb2z.png)
Main menu of application. You can search sites by query and get snippets and links with the most relevance sites

## How to run?▶️

- Make sure that you got java 17 version and MySql (started)
- Download last release https://github.com/GridyushkoF/SearchEngine/releases
- Download archive and application.yaml files
- Unpack archive and get jar-file
- important: application.yaml and jar-file must be in same folder!
- Configure applicaiton.yaml (check next step)
- Open command line in folder with jar-file and write command java -jar FILE_NAME.jar (where FILE_NAME.jar is jar-file name)
- Ready! Spring boot app will be started and opened in browser.

## How to make a config in application.yaml?⚙️

So, if you want co configure application.yaml, you should to make 3 little steps:

### 1 - configure sites will be indexed

Example you can see at root of git - application yaml.
yml indexing-settings:  
sites:

- url: https://www.playback.ru  
  name: playback
- url: https://dombulgakova.ru  
  name: dombulgakova
  urls and names can be other! Paste here your sites, where you
  want to search!

### 2 - configure the database

Download mySql and set up profile and schema
next you should to check application.yaml and add your cfg with similar structure like in example application.yaml
