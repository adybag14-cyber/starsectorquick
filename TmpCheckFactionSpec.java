public class TmpCheckFactionSpec {
  public static void main(String[] args) throws Exception {
    Class<?> cls = Class.forName("com.fs.starfarer.loading.if");
    Object spec = cls.getDeclaredConstructor().newInstance();
    java.lang.reflect.Method getDoctrine = cls.getMethod("getDoctrine");
    java.lang.reflect.Method getDisplayName = cls.getMethod("getDisplayName");
    java.lang.reflect.Method getFleetNames = cls.getMethod("getFleetNames");
    java.lang.reflect.Method getShipRoles = cls.getMethod("getShipRoles");
    java.lang.reflect.Method getRanksAndPosts = cls.getMethod("getRanksAndPosts");
    java.lang.reflect.Method getAllPortraits = null;
    for (java.lang.reflect.Method m : cls.getMethods()) {
      if ("getAllPortraits".equals(m.getName()) && m.getParameterTypes().length == 2) {
        getAllPortraits = m;
        break;
      }
    }
    System.out.println("doctrine=" + getDoctrine.invoke(spec));
    System.out.println("displayName=" + getDisplayName.invoke(spec));
    System.out.println("fleetNames=" + getFleetNames.invoke(spec));
    System.out.println("shipRoles=" + getShipRoles.invoke(spec));
    System.out.println("ranks=" + getRanksAndPosts.invoke(spec));
    if (getAllPortraits != null) {
      Class<?> cat = getAllPortraits.getParameterTypes()[0];
      Class<?> gender = Class.forName("com.fs.starfarer.api.characters.FullName$Gender");
      Object catVal = cat.isEnum() ? cat.getEnumConstants()[0] : null;
      @SuppressWarnings("unchecked")
      Class<? extends Enum> gEnum = (Class<? extends Enum>) gender.asSubclass(Enum.class);
      Object male = Enum.valueOf(gEnum, "MALE");
      Object female = Enum.valueOf(gEnum, "FEMALE");
      Object mList = getAllPortraits.invoke(spec, catVal, male);
      Object fList = getAllPortraits.invoke(spec, catVal, female);
      int mSize = (mList instanceof java.util.Collection) ? ((java.util.Collection<?>)mList).size() : -1;
      int fSize = (fList instanceof java.util.Collection) ? ((java.util.Collection<?>)fList).size() : -1;
      System.out.println("portraits male=" + mSize + " female=" + fSize);
    }
  }
}
