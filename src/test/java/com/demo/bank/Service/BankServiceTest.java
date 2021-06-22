package com.demo.bank.Service;

import com.demo.bank.constant.AccountStatus;
import com.demo.bank.model.entity.BankAccountsEntity;
import com.demo.bank.model.entity.BankBranchesEntity;
import com.demo.bank.model.entity.BankTransactionsEntity;
import com.demo.bank.model.request.BankTransactionRequest;
import com.demo.bank.model.request.BankTransferRequest;
import com.demo.bank.model.request.OpenBankAccountRequest;
import com.demo.bank.model.response.BankTransactionResponse;
import com.demo.bank.model.response.CommonResponse;
import com.demo.bank.model.response.ErrorResponse;
import com.demo.bank.model.response.OpenBankAccountResponse;
import com.demo.bank.repository.BankAccountsRepository;
import com.demo.bank.repository.BankBranchesRepository;
import com.demo.bank.repository.BankTransactionsRepository;
import com.demo.bank.repository.CustomerInformationRepository;
import com.demo.bank.service.BankService;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;


@ExtendWith(MockitoExtension.class)
public class BankServiceTest {

    @InjectMocks
    private BankService bankService;

    @Mock
    private BankAccountsRepository bankAccountsRepository;
    @Mock
    private BankBranchesRepository bankBranchesRepository;
    @Mock
    private BankTransactionsRepository bankTransactionsRepository;
    @Mock
    private CustomerInformationRepository customerInformationRepository;

    @BeforeEach
    public void init(){
        ReflectionTestUtils.setField(bankService,"openAccountAttempt",3);
    }

    @Test
    public void success_openBankAccount() {

        BankBranchesEntity bankBranchesEntity = new BankBranchesEntity();
        Integer resultBranchId = new Random().nextInt(10);
        bankBranchesEntity.setBranchId(resultBranchId);
        bankBranchesEntity.setBranchName("mockBranchName");
        Mockito.when(bankBranchesRepository.findAllByBranchName("mockBranchName")).thenReturn(bankBranchesEntity);

        BankAccountsEntity expectedResult = new BankAccountsEntity();
            expectedResult.setAccountId(UUID.randomUUID());
            expectedResult.setAccountBranchId(bankBranchesEntity.getBranchId());
            expectedResult.setAccountNumber("0123456789");
            expectedResult.setAccountName("mockAccountName");
            expectedResult.setAccountBalance(BigDecimal.ZERO);
            expectedResult.setAccountStatus(AccountStatus.ACTIVATED.getValue());
            Date date = Calendar.getInstance().getTime();
            expectedResult.setAccountCreatedDate(date);
            expectedResult.setAccountUpdatedDate(date);

        Mockito.when(bankAccountsRepository.save(any())).thenReturn(expectedResult);

        OpenBankAccountRequest openBankAccountRequest = new OpenBankAccountRequest();
        openBankAccountRequest.setName("mockAccountName");
        openBankAccountRequest.setBranchName(bankBranchesEntity.getBranchName());

        CommonResponse commonResponse = bankService.openBankAccount(openBankAccountRequest);

        OpenBankAccountResponse openBankAccountResponse = (OpenBankAccountResponse) commonResponse.getData();

        assertEquals(expectedResult.getAccountName(),openBankAccountResponse.getAccountName());
        assertEquals(expectedResult.getAccountNumber(),openBankAccountResponse.getAccountNumber());
        assertEquals(resultBranchId,openBankAccountResponse.getBranchId());
        assertEquals("SUCCESS",commonResponse.getStatus());
        assertEquals(HttpStatus.CREATED,commonResponse.getHttpStatus());

    }

    @Test
    public void fail_openBankAccount_duplicateAccountNumber(){

        BankBranchesEntity bankBranchesEntity = new BankBranchesEntity();
        Integer resultBranchId = new Random().nextInt(10);
        bankBranchesEntity.setBranchId(resultBranchId);
        bankBranchesEntity.setBranchName("mockBranchName");
        Mockito.when(bankBranchesRepository.findAllByBranchName("mockBranchName")).thenReturn(bankBranchesEntity);

        BankAccountsEntity bankAccountsEntity = new BankAccountsEntity();
        bankAccountsEntity.setAccountNumber("0123456789");
        Mockito.when(bankAccountsRepository.findAllByAccountNumber(anyString())).thenReturn(bankAccountsEntity);

        BankAccountsEntity expectedResult = new BankAccountsEntity();
        expectedResult.setAccountNumber("0123456789");

        OpenBankAccountRequest openBankAccountRequest = new OpenBankAccountRequest();
        openBankAccountRequest.setName("MockName");
        openBankAccountRequest.setDateOfBirth("12-02-1994");
        openBankAccountRequest.setAddress("MockAddress");
        openBankAccountRequest.setBranchName(bankBranchesEntity.getBranchName());

        CommonResponse commonResponse = bankService.openBankAccount(openBankAccountRequest);

        ErrorResponse errorResponse = (ErrorResponse) commonResponse.getData();

        assertEquals("ERROR",commonResponse.getStatus());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,commonResponse.getHttpStatus());
        assertEquals("ERROR",errorResponse.getError());

    }

    @Test
    public void fail_openBankAccount_notFoundBranchName(){

        BankBranchesEntity bankBranchesEntity = new BankBranchesEntity();
        bankBranchesEntity.setBranchName("MockBranchName");

        Mockito.when(bankBranchesRepository.findAllByBranchName(any())).thenReturn(null);

        OpenBankAccountRequest openBankAccountRequest = new OpenBankAccountRequest();
        openBankAccountRequest.setBranchName("MockBranchName");

        CommonResponse commonResponse = bankService.openBankAccount(openBankAccountRequest);

        ErrorResponse errorResponse = (ErrorResponse) commonResponse.getData();

        assertEquals("NOT_FOUND",commonResponse.getStatus());
        assertEquals(HttpStatus.NOT_FOUND,commonResponse.getHttpStatus());
        assertEquals("BRANCH NOT FOUND",errorResponse.getError());
    }

    @Test
    public void success_depositTransaction(){

        BankAccountsEntity bankAccountsEntity = new BankAccountsEntity();
        bankAccountsEntity.setAccountId(UUID.randomUUID());
        bankAccountsEntity.setAccountBranchId(1);
        bankAccountsEntity.setAccountName("MockAccountName");
        bankAccountsEntity.setAccountNumber("0123456789");
        bankAccountsEntity.setAccountBalance(BigDecimal.ZERO);
        bankAccountsEntity.setAccountStatus(AccountStatus.ACTIVATED.getValue());
        Date date = Calendar.getInstance().getTime();
        bankAccountsEntity.setAccountCreatedDate(date);
        bankAccountsEntity.setAccountUpdatedDate(date);
        Mockito.when(bankAccountsRepository.findAllByAccountNumberAndAccountStatus
                ("0123456789",AccountStatus.ACTIVATED.getValue())).thenReturn(bankAccountsEntity);

        BankTransactionsEntity bankTransactionsEntity = new BankTransactionsEntity();
        bankTransactionsEntity.setTransactionId(UUID.randomUUID());
        bankTransactionsEntity.setAccountId(UUID.randomUUID());
        bankTransactionsEntity.setTransactionAmount(BigDecimal.valueOf(500));
        bankTransactionsEntity.setTransactionType("DEPOSIT");
        bankTransactionsEntity.setTransactionDate(Calendar.getInstance().getTime());
        Mockito.when(bankTransactionsRepository.save(any())).thenReturn(bankTransactionsEntity);

        BankAccountsEntity updatedBankAccount = new BankAccountsEntity();
        updatedBankAccount.setAccountId(bankAccountsEntity.getAccountId());
        updatedBankAccount.setAccountBranchId(bankAccountsEntity.getAccountBranchId());
        updatedBankAccount.setAccountNumber(bankAccountsEntity.getAccountNumber());
        updatedBankAccount.setAccountName(bankAccountsEntity.getAccountName());
        updatedBankAccount.setAccountBalance(bankTransactionsEntity.getTransactionAmount());
        updatedBankAccount.setAccountStatus(bankAccountsEntity.getAccountStatus());
        updatedBankAccount.setAccountCreatedDate(bankAccountsEntity.getAccountCreatedDate());
        updatedBankAccount.setAccountUpdatedDate(Calendar.getInstance().getTime());

        BankTransactionRequest bankTransactionRequest = new BankTransactionRequest();
        bankTransactionRequest.setAccountName("MockAccountName");
        bankTransactionRequest.setAccountNumber("0123456789");
        bankTransactionRequest.setAmount(BigDecimal.valueOf(500));

        CommonResponse commonResponse = bankService.depositTransaction(bankTransactionRequest);

        BankTransactionResponse bankTransactionResponse = (BankTransactionResponse) commonResponse.getData();

        assertEquals("SUCCESS",commonResponse.getStatus());
        assertEquals(HttpStatus.CREATED,commonResponse.getHttpStatus());
        assertEquals("MockAccountName",bankTransactionResponse.getAccountName());
        assertEquals("0123456789",bankTransactionResponse.getAccountNumber());
        assertEquals(bankTransactionsEntity.getTransactionAmount(),bankTransactionResponse.getAmount());
        assertEquals(updatedBankAccount.getAccountBalance(),bankTransactionResponse.getAccountBalance());
        assertEquals(bankTransactionsEntity.getTransactionDate(),bankTransactionResponse.getTransactionDate());

    }

    @Test
    public void fail_depositTransaction_notFoundBankAccount(){
        BankAccountsEntity bankAccountsEntity = new BankAccountsEntity();
        bankAccountsEntity.setAccountId(UUID.randomUUID());
        bankAccountsEntity.setAccountBranchId(1);
        bankAccountsEntity.setAccountName("MockAccountName");
        bankAccountsEntity.setAccountNumber("0123456789");
        bankAccountsEntity.setAccountBalance(BigDecimal.ZERO);
        bankAccountsEntity.setAccountStatus(AccountStatus.ACTIVATED.getValue());
        Date date = Calendar.getInstance().getTime();
        bankAccountsEntity.setAccountCreatedDate(date);
        bankAccountsEntity.setAccountUpdatedDate(date);
        Mockito.when(bankAccountsRepository.findAllByAccountNumberAndAccountStatus
                ("0123456789",AccountStatus.ACTIVATED.getValue())).thenReturn(null);

        BankTransactionRequest bankTransactionRequest = new BankTransactionRequest();
        bankTransactionRequest.setAccountName("MockAccountName");
        bankTransactionRequest.setAccountNumber("0123456789");
        bankTransactionRequest.setAmount(BigDecimal.valueOf(500));

        CommonResponse commonResponse = bankService.depositTransaction(bankTransactionRequest);

        ErrorResponse errorResponse = (ErrorResponse) commonResponse.getData();

        assertEquals("NOT_FOUND",commonResponse.getStatus());
        assertEquals(HttpStatus.NOT_FOUND,commonResponse.getHttpStatus());
        assertEquals("BANK ACCOUNT NOT FOUND",errorResponse.getError());

    }
    

}