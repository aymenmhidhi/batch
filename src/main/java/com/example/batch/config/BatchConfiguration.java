package com.example.batch.config;

import com.example.batch.domain.Customer;
import com.example.batch.io.CustomerRecord;
import com.example.batch.io.ExcelCustomerItemReader;
import com.example.batch.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.batch.item.support.builder.RepositoryItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchConfiguration.class);

    @Bean
    @StepScope
    public MultiResourceItemReader<CustomerRecord> customerResourceReader(
            @Value("#{jobParameters['inputDirectory'] ?: '${app.batch.input-directory}'}") String inputDirectory,
            @Value("#{jobParameters['filePattern'] ?: '${app.batch.file-pattern:customer-*.xlsx}'}") String filePattern,
            ResourcePatternResolver resolver,
            ExcelCustomerItemReader delegate) {
        try {
            Resource[] resources = resolver.getResources("file:" + inputDirectory + "/" + filePattern);
            if (resources.length == 0) {
                LOGGER.warn("Aucun fichier Excel trouvé dans {} avec le pattern {}", inputDirectory, filePattern);
            }
            return new MultiResourceItemReaderBuilder<CustomerRecord>()
                    .name("customerMultiResourceReader")
                    .resources(resources)
                    .delegate(delegate)
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to resolve Excel resources in directory " + inputDirectory, e);
        }
    }

    @Bean
    @StepScope
    public ExcelCustomerItemReader excelCustomerItemReader() {
        return new ExcelCustomerItemReader();
    }

    @Bean
    public ItemProcessor<CustomerRecord, Customer> customerItemProcessor(CustomerRepository repository) {
        return new CustomerItemProcessor(repository);
    }

    @Bean
    public ItemWriter<Customer> customerItemWriter(CustomerRepository repository) {
        return new RepositoryItemWriterBuilder<Customer>()
                .repository(repository)
                .methodName("save")
                .build();
    }

    @Bean
    public Step importCustomersStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    MultiResourceItemReader<CustomerRecord> customerResourceReader,
                                    ItemProcessor<CustomerRecord, Customer> customerItemProcessor,
                                    ItemWriter<Customer> customerItemWriter) {
        return new org.springframework.batch.core.step.builder.StepBuilder("importCustomersStep", jobRepository)
                .<CustomerRecord, Customer>chunk(50, transactionManager)
                .reader(customerResourceReader)
                .processor(customerItemProcessor)
                .writer(customerItemWriter)
                .build();
    }

    @Bean
    public Job importCustomersJob(JobRepository jobRepository,
                                  Step importCustomersStep) {
        return new JobBuilder("importCustomersJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(importCustomersStep)
                .build();
    }
}
