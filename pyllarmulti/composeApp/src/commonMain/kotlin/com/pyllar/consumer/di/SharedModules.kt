package com.pyllar.consumer.di

import com.pyllar.consumer.config.getApiBaseUrl
import com.pyllar.consumer.data.remote.datasource.AuthRemoteDataSource
import com.pyllar.consumer.data.remote.datasource.AuthRemoteDataSourceImpl
import com.pyllar.consumer.data.remote.network.PyllarApiClient
import com.pyllar.consumer.data.repository.AuthRepositoryImpl
import com.pyllar.consumer.data.repository.DashboardRepositoryImpl
import com.pyllar.consumer.data.repository.FundDetailsRepositoryImpl
import com.pyllar.consumer.data.repository.MutualFundRepositoryImpl
import com.pyllar.consumer.data.repository.OnboardingRepositoryImpl
import com.pyllar.consumer.data.repository.RedemptionRepositoryImpl
import com.pyllar.consumer.data.repository.ReferralRepositoryImpl
import com.pyllar.consumer.domain.repository.AuthRepository
import com.pyllar.consumer.domain.repository.DashboardRepository
import com.pyllar.consumer.domain.repository.FundDetailsRepository
import com.pyllar.consumer.domain.repository.MutualFundRepository
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.domain.repository.RedemptionRepository
import com.pyllar.consumer.domain.repository.ReferralRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.presentation.auth.login.AuthViewModel
import com.pyllar.consumer.presentation.auth.permission.PermissionViewModel
import com.pyllar.consumer.presentation.auth.phone.OtpVerificationViewModel
import com.pyllar.consumer.presentation.auth.phone.PhoneVerificationViewModel
import com.pyllar.consumer.presentation.dashboard.DashboardViewModel
import com.pyllar.consumer.presentation.dashboard.InitialDashboardViewModel
import com.pyllar.consumer.presentation.dashboard.InvestmentDashboardV2ViewModel
import com.pyllar.consumer.presentation.dashboard.SchemeDetailsViewModel
import com.pyllar.consumer.presentation.dashboard.WithdrawAmountViewModel
import com.pyllar.consumer.presentation.dashboard.WithdrawViewModel
import com.pyllar.consumer.presentation.dashboard.WithdrawSuccessViewModelV2
import com.pyllar.consumer.presentation.mutualfund.onboarding.OnboardingViewModel
import com.pyllar.consumer.presentation.mutualfund.portfolio.PortfolioViewModel
import com.pyllar.consumer.presentation.mutualfund.sip.SipViewModel
import com.pyllar.consumer.presentation.mutualfund.details.FundDetailsViewModel
import com.pyllar.consumer.presentation.mutualfund.onboarding.MinDetailsViewModel
import com.pyllar.consumer.presentation.mutualfund.onboarding.NameDobViewModel
import com.pyllar.consumer.presentation.mutualfund.onboarding.NomineeDetailsViewModel
import com.pyllar.consumer.presentation.mutualfund.onboarding.PanKycViewModel
import com.pyllar.consumer.presentation.mutualfund.onboarding.AdditionalKycViewModel
import com.pyllar.consumer.presentation.mutualfund.onboarding.BankDetailsViewModel
import com.pyllar.consumer.presentation.mutualfund.onboarding.SipAmountScreenV2ViewModel
import com.pyllar.consumer.presentation.support.HelperCodeViewModel
import com.pyllar.consumer.presentation.mutualfund.onboarding.PreVerificationViewModel
import com.pyllar.consumer.presentation.mutualfund.onboarding.SignatureViewModel
import com.pyllar.consumer.domain.repository.PreVerificationRepository
import com.pyllar.consumer.data.repository.PreVerificationRepositoryImpl
import com.pyllar.consumer.presentation.mutualfund.onboarding.MandateAuthModel
import com.pyllar.consumer.presentation.mutualfund.onboarding.LumpsumPurchaseAuthViewModel
import com.pyllar.consumer.presentation.mutualfund.onboarding.CheckPanPopulatedDetailsViewModel
import com.pyllar.consumer.presentation.profile.ProfileViewModel
import com.pyllar.consumer.navigation.ForceUpdateManager
import com.pyllar.consumer.presentation.referral.ReferralViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Shared Koin module for networking and repositories.
 *
 * Platform modules must provide SessionStore.
 */
val sharedModule: Module = module {
    single { ForceUpdateManager() }
    single { PyllarApiClient(getApiBaseUrl()) }
    single { com.pyllar.consumer.data.remote.crypto.createSecureSessionStore() }
    single { com.pyllar.consumer.data.remote.crypto.SecureHandshakeCoordinator({ getApiBaseUrl() }, get(), get()) }
    single<AuthRemoteDataSource> { AuthRemoteDataSourceImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get<SessionStore>(), get()) }
    single<OnboardingRepository> { OnboardingRepositoryImpl(get(), get()) }
    single<MutualFundRepository> { MutualFundRepositoryImpl(get()) }
    single<FundDetailsRepository> { FundDetailsRepositoryImpl(get()) }
    single<RedemptionRepository> { RedemptionRepositoryImpl(get()) }
    single<DashboardRepository> { DashboardRepositoryImpl(get()) }
    single<ReferralRepository> { ReferralRepositoryImpl(get()) }
    single<com.pyllar.consumer.domain.repository.CommonRepository> { com.pyllar.consumer.data.repository.CommonRepositoryImpl(get()) }
    single<PreVerificationRepository> { PreVerificationRepositoryImpl(get()) }
    single<com.pyllar.consumer.domain.repository.UpiRepository> { com.pyllar.consumer.data.repository.UpiRepositoryImpl(get()) }

    // Auth ViewModels
    factoryOf(::PhoneVerificationViewModel)
    factoryOf(::OtpVerificationViewModel)
    factoryOf(::PermissionViewModel)
    factoryOf(::AuthViewModel)

    // Mutual Fund ViewModels
    factoryOf(::OnboardingViewModel)
    factoryOf(::PortfolioViewModel)
    factoryOf(::SipViewModel)
    factoryOf(::FundDetailsViewModel)
    factoryOf(::MinDetailsViewModel)
    factoryOf(::NameDobViewModel)
    factoryOf(::NomineeDetailsViewModel)
    factoryOf(::PanKycViewModel)
    factoryOf(::AdditionalKycViewModel)
    factoryOf(::SignatureViewModel)
    factoryOf(::BankDetailsViewModel)
    factoryOf(::SipAmountScreenV2ViewModel)
    factoryOf(::PreVerificationViewModel)
    factoryOf(::CheckPanPopulatedDetailsViewModel)
    factory { MandateAuthModel(get(), CoroutineScope(Dispatchers.Main)) }
    factoryOf(::LumpsumPurchaseAuthViewModel)
    factory { WithdrawAmountViewModel(get(), get()) }
    factory { com.pyllar.consumer.presentation.mutualfund.upi.UpiAccountLinkingViewModel(get()) }

    // Dashboard ViewModels
    factoryOf(::DashboardViewModel)
    factoryOf(::InitialDashboardViewModel)
    factoryOf(::InvestmentDashboardV2ViewModel)
    factoryOf(::SchemeDetailsViewModel)
    factoryOf(::WithdrawViewModel)
    factoryOf(::HelperCodeViewModel)
    factoryOf(::ProfileViewModel)
    factoryOf(::ReferralViewModel)
    factoryOf(::WithdrawSuccessViewModelV2)
}
