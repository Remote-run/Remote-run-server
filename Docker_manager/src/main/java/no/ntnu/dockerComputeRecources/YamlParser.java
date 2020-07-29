package no.ntnu.dockerComputeRecources;

import no.ntnu.dockerComputeRecources.ResourceTypes.ComputeResource;
import no.ntnu.dockerComputeRecources.ResourceTypes.CpuResource;
import no.ntnu.dockerComputeRecources.ResourceTypes.GpuResource;
import no.ntnu.dockerComputeRecources.ResourceTypes.RamResource;
import no.ntnu.util.DebugLogger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class YamlParser {

    /*
    TODO: the yaml file shold really just be read in to the db or mark the ticket with resource needed
           - muligens endre så den her bar les av fra fil og dytte inn i db hvis den blir etterspurt en den ikke har fra før av
           - hvis db blir brukt må resjussj og ticket styre fikses på nytt spesielt mtp slave manageran
     */


    private static final DebugLogger dbl = new DebugLogger(false);

    private static final File resourceKeysFile = new File("/app/resource_tags.yaml");
    private static final File systemResourceFile = new File("/app/system_resources.yaml");


    private final boolean readOnEveryTicket = true;
    private final ResourceKey defaultKey = new ResourceKey("Default",1,-1,-1);
    private Keys resourceKeys = new Keys();

    private void readResourceKeys(){
        try {
            InputStream inputStream = new FileInputStream(resourceKeysFile);
            Yaml yaml = new Yaml();
            resourceKeys = yaml.loadAs(inputStream, Keys.class);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public HashMap<ResourceType, Integer> translateTicketKey(String key){
        HashMap<ResourceType, Integer> returnMap = null;
        if (readOnEveryTicket || this.resourceKeys.keys.isEmpty()){
            this.readResourceKeys();
        }

        for (ResourceKey resourceKey:  this.resourceKeys.keys){
            if (resourceKey.id.equals(key)){
                returnMap = resourceKey.mapToKeyValue();
                break;
            }
        }

        if (returnMap == null){
            returnMap = this.defaultKey.mapToKeyValue();
        }

        return returnMap;
    }

    public static ComputeResource[] getSystemResources(){
        // TODO: this shold super not be here
        ResourceKey systemResources = new ResourceKey();
        try {
            InputStream inputStream = new FileInputStream(systemResourceFile);
            Yaml yaml = new Yaml();
            systemResources = yaml.loadAs(inputStream, ResourceKey.class);
        } catch (Exception e){
            e.printStackTrace();
        }


        return systemResources.mapToComputeResource();
    }



    private static class Keys{
        public List<YamlParser.ResourceKey> keys;
    }

    private static class ResourceKey {
        public String id;
        public int GPU;
        public int CPU;
        public int GIG_MEMORY;

        public ResourceKey(){

        }

        public ResourceKey(String id, int GPU, int CPU, int GIG_MEMORY) {
            this.id = id;
            this.GPU = GPU;
            this.CPU = CPU;
            this.GIG_MEMORY = GIG_MEMORY;
        }

        public HashMap<ResourceType, Integer> mapToKeyValue(){
            HashMap<ResourceType, Integer> ret = new HashMap<>();


            if (GPU != -1){
                ret.put(ResourceType.GPU, this.GPU);
            }
            if (CPU != -1){
                ret.put(ResourceType.CPU, this.CPU);
            }
            if (GIG_MEMORY != -1){
                ret.put(ResourceType.RAM, this.GIG_MEMORY);
            }
            return ret;
        }

        public ComputeResource[] mapToComputeResource(){
            Vector<ComputeResource> computeResources = new Vector<>();

            if (GPU != -1){
                dbl.log("num gpus:",GPU);
                computeResources.add(new GpuResource(GPU));
            }
            if (CPU != -1){
                dbl.log("num CPU:",CPU);
                computeResources.add(new CpuResource(CPU));

            }
            if (GIG_MEMORY != -1){
                dbl.log("num RAM:",GIG_MEMORY);
                computeResources.add(new RamResource(GIG_MEMORY));
            }

            return computeResources.toArray(ComputeResource[]::new);
        }
    }

}
