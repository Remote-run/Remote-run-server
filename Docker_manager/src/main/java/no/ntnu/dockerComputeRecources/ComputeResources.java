package no.ntnu.dockerComputeRecources;

import no.ntnu.dockerComputeRecources.ResourceTypes.*;
import no.ntnu.sql.SystemDbFunctions;
import no.trygvejw.debugLogger.DebugLogger;
import org.postgresql.util.JdbcBlackHole;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;

public class ComputeResources {
    private static final File resourceKeysFile = new File("/app/resource_tags.yaml");
    private static final File systemResourceFile = new File("/app/system_resources.yaml");
    public  static final ResourceKey defaultKey = new ResourceKey("DEFAULT",1,-1,-1);
    private static final Vector<ResourceKey> keys = new Vector<>();

    private static final DebugLogger dbl = new DebugLogger(false);

    public static ResourceKey getSystemResourceKey(){
        ResourceKey resources = null;
        try {
            InputStream inputStream = new FileInputStream(systemResourceFile);
            Yaml yaml = new Yaml();
            ResourceKey key = yaml.loadAs(inputStream, ResourceKey.class);
            resources  = key;
        } catch (Exception e){
            e.printStackTrace();
        }
        return resources;

    }

    private static Vector<ResourceKey> ReadYamlResourceKeys(){
        dbl.log("READING YAML KEYS");
        Vector<ResourceKey> ret = new Vector<>();
        try {
            InputStream inputStream = new FileInputStream(resourceKeysFile);
            Yaml yaml = new Yaml();
            ret = new Vector(yaml.loadAs(inputStream, YamlListHolder.class).keys);
        } catch (Exception e){
            e.printStackTrace();
        }
        return ret;
    }

    private static void updateLocalKeys(){
        try {
            Vector<ResourceKey> mabyeNewDbKeys = new Vector(Arrays.asList(SystemDbFunctions.getDbTicketResourceKeys()));
            Vector<ResourceKey> mabyeNewYamlKeys = ReadYamlResourceKeys();

            dbl.log(mabyeNewDbKeys.toString());
            dbl.log(mabyeNewYamlKeys.toString());

            mabyeNewYamlKeys.stream()
                    .peek( resourceKey ->  dbl.log("ney key peek pre filter", resourceKey.resourceId))
                    .filter(resourceKey -> mabyeNewDbKeys.stream().map(k -> k.resourceId).noneMatch(k -> resourceKey.resourceId.equals(k)))// Yes, this is horrible
                    .peek( resourceKey ->  dbl.log("ney key peek post filter", resourceKey.resourceId))
                    .forEach(SystemDbFunctions::addResourceKey);


            Stream.of(mabyeNewDbKeys, mabyeNewYamlKeys)
                    .flatMap(Collection::stream)
                    .distinct()
                    .filter(resourceKey -> !keys.contains(resourceKey))
                    .forEach(keys::add);
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Trys to translate the provided resource key string to a Resource key object
     * the database is queried for the key, if it is not there the default key is used
     *
     * the method is synchronized to avoid sync issues with updating the cashed sync tables
     *
     * @param key the key to translate
     * @return a resource key object
     */
    public static synchronized ResourceKey TranslateComputeResourceKey(String key){
        dbl.log("translating key:", key);
        ResourceKey returnKey = null;

        if (keys.stream().noneMatch(resourceKey -> resourceKey.resourceId.equals(key))){
            dbl.log("not in local keys", key);
            updateLocalKeys();
        }

        ResourceKey[] resourceKeys = keys.stream().filter(resourceKey -> resourceKey.resourceId.equals(key)).toArray(ResourceKey[]::new);
        dbl.log("Key options: ", resourceKeys);
        returnKey = (resourceKeys.length > 0)? resourceKeys[0]: defaultKey;
        return returnKey;
    }

    public static HashMap<ResourceType, Integer> mapUnitToTypeMap(ResourceKey resource){
        HashMap<ResourceType, Integer> ret = new HashMap<>();


        if (resource.gpuSlots != -1){
            ret.put(ResourceType.GPU, resource.gpuSlots);
        }
        if (resource.cpus != -1){
            ret.put(ResourceType.CPU, resource.cpus);
        }
        if (resource.gigRam != -1){
            ret.put(ResourceType.RAM, resource.gigRam);
        }
        return ret;
    }

    public static ComputeResource[] mapUnitToComputeResource(ResourceKey resource){
        Vector<ComputeResource> computeResources = new Vector<>();

        if (resource.gpuSlots != -1){
            computeResources.add(new GpuResource(resource.gpuSlots));
        }
        if (resource.cpus != -1){
            computeResources.add(new CpuResource(resource.cpus));

        }
        if (resource.gigRam != -1){
            computeResources.add(new RamResource(resource.gigRam));
        }

        return computeResources.toArray(ComputeResource[]::new);
    }

    private static class YamlListHolder{
        public YamlListHolder(){}
        public List<ResourceKey> keys;
    }

    public static class ResourceKey {
        public String resourceId;
        public int gpuSlots;
        public int cpus;
        public int gigRam;

        public ResourceKey(){}

        public ResourceKey(String resourceId, int gpuSlots, int cpus, int gigRam) {
            this.resourceId = resourceId;
            this.gpuSlots = gpuSlots;
            this.cpus = cpus;
            this.gigRam = gigRam;
        }


    }
}
