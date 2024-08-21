package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.*;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.LogGroupProps;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

import java.util.*;

public class ProductServiceStack extends Stack {

    public ProductServiceStack(final Construct scope, final String id, final StackProps props, ProductServiceProps productServiceProps) {
        super(scope, id, props);

        FargateTaskDefinition fargateTaskDefinition = new FargateTaskDefinition(this, "TaskDefinition", FargateTaskDefinitionProps.builder()
                .family("product-service")
                .cpu(512)
                .memoryLimitMiB(1024)
                .build());

        AwsLogDriver logDriver = new AwsLogDriver(AwsLogDriverProps.builder()
                .logGroup(new LogGroup(this, "LogGroup", LogGroupProps.builder()
                        .logGroupName("ProductService")
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .retention(RetentionDays.ONE_MONTH)
                        .build()))
                .streamPrefix("ProductService")
                .build());

        Map<String, String> envVariables = new HashMap<>();
        envVariables.put("SERVER_PORT", "8080");

        fargateTaskDefinition.addContainer("ProductServiceContainer", ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromEcrRepository(productServiceProps.repository(), "1.0.0"))
                .containerName("productService")
                .logging(logDriver)
                .portMappings(Collections.singletonList(PortMapping.builder()
                        .containerPort(8080)
                        .protocol(Protocol.TCP)
                        .build()))
                .environment(envVariables)
                .build());

        ApplicationListener applicationListener = productServiceProps.applicationLoadBalancer()
                .addListener("ProductServiceAlbListener", ApplicationListenerProps.builder()
                        .port(8080)
                        .protocol(ApplicationProtocol.HTTP)
                        .loadBalancer(productServiceProps.applicationLoadBalancer())
                        .build());

        FargateService fargateService = new FargateService(this, "ProductService", FargateServiceProps.builder()
                .serviceName("ProductService")
                .cluster(productServiceProps.cluster())
                .taskDefinition(fargateTaskDefinition)
                .desiredCount(2)
                .assignPublicIp(false)
                .build());

        productServiceProps.repository().grantPull(Objects.requireNonNull(fargateTaskDefinition.getExecutionRole()));
        fargateService.getConnections().getSecurityGroups().get(0).addEgressRule(Peer.anyIpv4(), Port.tcp(8080)); // security group

        applicationListener.addTargets("ProductServiceAlbTarget", AddApplicationTargetsProps.builder()
                .targetGroupName("productServiceAlb")
                .port(8080)
                .protocol(ApplicationProtocol.HTTP)
                .targets(Collections.singletonList(fargateService))
                .deregistrationDelay(Duration.seconds(30))
                .healthCheck(HealthCheck.builder()
                        .enabled(true)
                        .interval(Duration.seconds(30))
                        .timeout(Duration.seconds(10))
                        .path("/actuator/health")
                        .port("8080")
                        .build())
                .build());

        NetworkListener networkListener = productServiceProps.networkLoadBalancer()
                .addListener("ProductServiceNlbListener", BaseNetworkListenerProps.builder()
                        .port(8080)
                        .protocol(software.amazon.awscdk.services.elasticloadbalancingv2.Protocol.TCP)
                        .build());

        networkListener.addTargets("ProductServiceNlbTarget", AddNetworkTargetsProps.builder()
                .port(8080)
                .protocol(software.amazon.awscdk.services.elasticloadbalancingv2.Protocol.TCP)
                .targetGroupName("productServiceNlb")
                .targets(Collections.singletonList(
                        fargateService.loadBalancerTarget(LoadBalancerTargetOptions.builder()
                                .containerName("productService")
                                .containerPort(8080)
                                .protocol(Protocol.TCP)
                                .build())
                ))
                .build());
    }
}

record ProductServiceProps(
        Vpc vpc,
        Cluster cluster,
        NetworkLoadBalancer networkLoadBalancer,
        ApplicationLoadBalancer applicationLoadBalancer,
        Repository repository
) {
}