package io.github.coffeelibs.maven.jextract;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class DumpIncludesParser {

    
    public static final String TYPE_FUNCTION="--include-function"; //          name of function to include                                  
    public static final String TYPE_CONSTANT="--include-constant"; //          name of macro or enum constant to include
    public static final String TYPE_STRUCT="--include-struct"; //            name of struct definition to include
    public static final String TYPE_TYPEDEF="--include-typedef"; //           name of type definition to include
    public static final String TYPE_UNION="--include-union"; //             name of union definition to include
    public static final String TYPE_VAR="--include-var"; //               name of global variable to include

    public static List<Include> parse(File file) {
        List<Include> includes=new ArrayList<>();
        try(Scanner scanner=new Scanner(new FileReader(file, StandardCharsets.UTF_8))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] items = line.split(" ");
                Include include = new Include();
                include.setPrefix(items[0]);
                include.setName(items[1]);
                include.setHeaderFile(items[items.length - 1]);
                includes.add(include);
            }
        }catch (IOException e){
            throw new RuntimeException("",e);
        }
        return includes;
    }


    public static List<Include> parse(File file, Map<String,String[]> filters) {
        List<Include> includes=new ArrayList<>();
        try(Scanner scanner=new Scanner(new FileReader(file, StandardCharsets.UTF_8))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] items = line.split(" ");
                Include include = new Include();
                include.setPrefix(items[0]);
                include.setName(items[1]);
                include.setHeaderFile(items[items.length - 1]);

                String[] filter=filters.get(include.getPrefix());
                if(isAllow(filter,include.getName())) {
                    includes.add(include);
                }
            }
        }catch (IOException e){
            throw new RuntimeException("",e);
        }
        return includes;
    }

    public static boolean isAllow(String [] filter,String name){
        if(filter == null){
            return false;
        }
        for(String f:filter){
            if("*".equals(f)){
                return true;
            }
            if(name.contains(f)){
                return true;
            }
        }
        return false;
    }

    public static void toFile(List<Include> includes,File target) throws IOException {
        FileWriter fileWriter=new FileWriter(target);
        includes.forEach(include -> {
            try {
                fileWriter.write(include.toRaw());
                fileWriter.write(System.lineSeparator());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    public static class Include{

        private String headerFile;
        private String prefix;
        private String name;

        public String getHeaderFile() {
            return headerFile;
        }

        public void setHeaderFile(String headerFile) {
            this.headerFile = headerFile;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String toRaw(){
            return format.formatted(prefix,name,headerFile);
        }
        //--include-function alloca    # header: /usr/include/alloca.h
        static String format="%s %s    # header: %s";
    }


    


}
