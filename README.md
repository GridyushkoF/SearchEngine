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
- Configure YML-file (check next step)
- Check, that you have java 17 where running and running MySql.
- Download last realease from this repository (jar-file with application.yaml)
- Make sure, that you got application.yaml and java application in SAME FOLDER
- Run it with command line: java -jar path_to_jar_file
(where path_to_jar_file - your path to realease)
- Next spring app will started. If it contains some exceptions, make sure, that you do all correct
- Application started and will be open in the browser



## How to make a config in app.yaml?⚙️

So, if you want co configure application.yaml, you should to make 2 little steps:

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
  ![image](https://github.com/user-attachments/assets/92322bfb-cf5b-4f6b-985f-a4c56a356ee4)

### 2 - configure the database

Download mySql and set up profile and schema
next you should to check application.yaml and add your cfg with similar structure like in example application.yaml
