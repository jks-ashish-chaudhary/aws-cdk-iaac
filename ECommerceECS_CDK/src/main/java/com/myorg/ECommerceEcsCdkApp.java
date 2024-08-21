package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

import java.util.HashMap;
import java.util.Map;

public class ECommerceEcsCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        Environment environment = Environment.builder()
                .account("**********")
                .region("us-east-1")
                .build();

        Map<String, String> infraTags = new HashMap<>();
        infraTags.put("team", "SiecolaCode");
        infraTags.put("cost", "ECommerceInfra");

        ECRStack ecrStack = new ECRStack(app, "Ecr", StackProps.builder()
                .env(environment)
                .tags(infraTags)
                .build());

        VPCStack vpcStack = new VPCStack(app, "Vpc", StackProps.builder()
                .env(environment)
                .tags(infraTags)
                .build());

        ClusterStack clusterStack = new ClusterStack(app, "Cluster", StackProps.builder()
                .env(environment)
                .tags(infraTags)
                .build(), new ClusterStackProps(vpcStack.getVpc()));
        clusterStack.addDependency(vpcStack); // this is the way to define dependency stack

        NlbStack nlbStack = new NlbStack(app, "Nlb", StackProps.builder()
                .env(environment)
                .tags(infraTags)
                .build(), new NlbStackProps(vpcStack.getVpc()));
        nlbStack.addDependency(vpcStack);

        Map<String, String> productServiceTags = new HashMap<>();
        productServiceTags.put("team", "SiecolaCode");
        productServiceTags.put("cost", "ProductService");

        ProductServiceStack productServiceStack = new ProductServiceStack(app, "ProductService",
                StackProps.builder()
                        .env(environment)
                        .tags(productServiceTags)
                        .build(),
                new ProductServiceProps(
                        vpcStack.getVpc(),
                        clusterStack.getCluster(),
                        nlbStack.getNetworkLoadBalancer(),
                        nlbStack.getApplicationLoadBalancer(),
                        ecrStack.getProductServiceRepository()));
        productServiceStack.addDependency(vpcStack);
        productServiceStack.addDependency(clusterStack);
        productServiceStack.addDependency(nlbStack);
        productServiceStack.addDependency(ecrStack);

        app.synth();
    }
}

