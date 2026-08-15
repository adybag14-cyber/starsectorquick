public final class TestRulesLiteralStringCleanup {
    private static void eq(String label,String a,String b){if(!a.equals(b))throw new AssertionError(label+" old="+show(a)+" new="+show(b));}
    private static String show(String s){return s.replace("\r","<CR>").replace("\n","<LF>");}
    public static void main(String[] args){
        String[] samples={"plain","a\rb","a\r\rb","a\nb","a\n\nb","a\r\nb","\rstart\n","end\r\n"};
        for(String s:samples){
            eq("CR "+show(s),s.replaceAll("\\r",""),s.replace("\r",""));
            eq("LF "+show(s),s.replaceAll("\\n"," "),s.replace("\n"," "));
        }
        System.out.println("TestRulesLiteralStringCleanup: OK regex/literal CR/LF semantics match");
    }
}
