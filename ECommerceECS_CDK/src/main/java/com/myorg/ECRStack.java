package com.myorg;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecr.RepositoryProps;
import software.amazon.awscdk.services.ecr.TagMutability;
import software.constructs.Construct;

public class ECRStack extends Stack {

    private final Repository productServiceRepository;

    public ECRStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        productServiceRepository = new Repository(this, "ProductService", RepositoryProps.builder()
                .repositoryName("productservice")
                .removalPolicy(RemovalPolicy.DESTROY)
                .imageTagMutability(TagMutability.IMMUTABLE)
                .emptyOnDelete(true)
                .build());
    }

    public Repository getProductServiceRepository() {
        return productServiceRepository;
    }
}
