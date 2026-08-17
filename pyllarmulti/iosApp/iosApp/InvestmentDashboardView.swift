import SwiftUI
import UIKit
import ComposeApp

class DashboardHostingController: UIHostingController<InvestmentDashboardView> {
    var state: InvestmentDashboardV2State {
        didSet {
            self.rootView = InvestmentDashboardView(
                state: state,
                onNavigateToProfile: onNavigateToProfile,
                onNavigateToHelp: onNavigateToHelp,
                onShareClick: onShareClick,
                onRateUsClick: onRateUsClick,
                onGoalClick: onGoalClick,
                onRecommendedGoalClick: onRecommendedGoalClick,
                onRefresh: onRefresh
            )
        }
    }
    
    var onNavigateToProfile: () -> Void
    var onNavigateToHelp: () -> Void
    var onShareClick: () -> Void
    var onRateUsClick: () -> Void
    var onGoalClick: (InvestmentGoal) -> Void
    var onRecommendedGoalClick: (InvestmentGoal) -> Void
    var onRefresh: () -> Void
    
    init(
        state: InvestmentDashboardV2State,
        onNavigateToProfile: @escaping () -> Void,
        onNavigateToHelp: @escaping () -> Void,
        onShareClick: @escaping () -> Void,
        onRateUsClick: @escaping () -> Void,
        onGoalClick: @escaping (InvestmentGoal) -> Void,
        onRecommendedGoalClick: @escaping (InvestmentGoal) -> Void,
        onRefresh: @escaping () -> Void
    ) {
        self.state = state
        self.onNavigateToProfile = onNavigateToProfile
        self.onNavigateToHelp = onNavigateToHelp
        self.onShareClick = onShareClick
        self.onRateUsClick = onRateUsClick
        self.onGoalClick = onGoalClick
        self.onRecommendedGoalClick = onRecommendedGoalClick
        self.onRefresh = onRefresh
        
        super.init(rootView: InvestmentDashboardView(
            state: state,
            onNavigateToProfile: onNavigateToProfile,
            onNavigateToHelp: onNavigateToHelp,
            onShareClick: onShareClick,
            onRateUsClick: onRateUsClick,
            onGoalClick: onGoalClick,
            onRecommendedGoalClick: onRecommendedGoalClick,
            onRefresh: onRefresh
        ))
    }
    
    @MainActor required dynamic init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

class DashboardWrapperView: UIView {
    let hostingController: DashboardHostingController
    
    init(hostingController: DashboardHostingController) {
        self.hostingController = hostingController
        super.init(frame: .zero)
        
        let subview = hostingController.view!
        subview.translatesAutoresizingMaskIntoConstraints = false
        addSubview(subview)
        NSLayoutConstraint.activate([
            subview.topAnchor.constraint(equalTo: self.topAnchor),
            subview.bottomAnchor.constraint(equalTo: self.bottomAnchor),
            subview.leadingAnchor.constraint(equalTo: self.leadingAnchor),
            subview.trailingAnchor.constraint(equalTo: self.trailingAnchor)
        ])
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

// Color Palette matching the Pyllar V2 brand theme
struct PyllarTheme {
    static let obsidian = Color(red: 0.08, green: 0.08, blue: 0.08) // V2Obsidian
    static let softGreen = Color(red: 0.06, green: 0.21, blue: 0.13) // Soft Forest Green
    static let successGreen = Color(red: 0.15, green: 0.65, blue: 0.35) // Success Green
    static let cream = Color(red: 0.98, green: 0.98, blue: 0.96) // V2Cream
    static let white = Color.white
    static let border = Color(red: 0.90, green: 0.90, blue: 0.88)
    static let textDark = Color(red: 0.12, green: 0.12, blue: 0.12)
    static let textGray = Color(red: 0.50, green: 0.50, blue: 0.50)
}

struct InvestmentDashboardView: View {
    // State values mapped from KMP or local state
    let state: InvestmentDashboardV2State
    
    // Callbacks for user actions matching Kotlin navigation parameters
    var onNavigateToProfile: () -> Void = {}
    var onNavigateToHelp: () -> Void = {}
    var onShareClick: () -> Void = {}
    var onRateUsClick: () -> Void = {}
    var onGoalClick: (InvestmentGoal) -> Void = { _ in }
    var onRecommendedGoalClick: (InvestmentGoal) -> Void = { _ in }
    var onRefresh: () -> Void = {}
    
    var body: some View {
        ZStack {
            // Gradient background matching Canvas drawRect in Compose
            VStack(spacing: 0) {
                LinearGradient(
                    colors: [PyllarTheme.obsidian, PyllarTheme.softGreen, PyllarTheme.cream],
                    startPoint: .top,
                    endPoint: .center
                )
                .frame(maxHeight: 400)
                PyllarTheme.cream
            }
            .ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 16) {
                    Spacer().frame(height: 10)
                    
                    // 1. User Header
                    UserHeaderView(
                        userName: state.userName,
                        isLoading: state.isLoading,
                        onProfileClick: onNavigateToProfile,
                        onHelpClick: onNavigateToHelp,
                        onShareClick: onShareClick,
                        onRateUsClick: onRateUsClick
                    )
                    
                    // 2. Combined Dashboard Card
                    CombinedDashboardCardView(
                        totalValue: state.totalValue,
                        profitLoss: state.profitLoss,
                        profitLossPercentage: state.profitLossPercentage,
                        isLoading: state.isLoading
                    )
                    
                    // 3. KYC Status Banner (If KYC is Pending/Rejected etc)
                    if !state.kycStatus.isEmpty && state.kycStatus != "SUCCESS" {
                        KycStatusBanner(status: state.kycStatus)
                    }
                    
                    // 4. Active Goals Section
                    if !state.primaryGoals.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Your Active Goals")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(PyllarTheme.textDark)
                                .padding(.horizontal)
                            
                            ForEach(state.primaryGoals, id: \.goalId) { goal in
                                ActiveGoalCardView(goal: goal, onClick: {
                                    onGoalClick(goal)
                                })
                                .padding(.horizontal)
                            }
                        }
                    } else if !state.isLoading && state.kycStatus == "SUCCESS" {
                        // KycApprovedReadyToInvestCard equivalent
                        ReadyToInvestCard()
                            .padding(.horizontal)
                    }
                    
                    // 5. Milestone Banner
                    if state.hasFirstMilestone && !state.milestoneMessage.isEmpty {
                        MilestoneCard(message: state.milestoneMessage)
                            .padding(.horizontal)
                    }
                    
                    // 6. Recommended / Next Goals Grid
                    if !state.recommendedGoals.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Explore Investment Options")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(PyllarTheme.textDark)
                                .padding(.horizontal)
                            
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 16) {
                                    ForEach(state.recommendedGoals, id: \.goalId) { goal in
                                        RecommendedGoalCard(goal: goal, onClick: {
                                            onRecommendedGoalClick(goal)
                                        })
                                    }
                                }
                                .padding(.horizontal)
                            }
                        }
                    }
                    
                    Spacer().frame(height: 40)
                }
            }
            .refreshable {
                onRefresh()
            }
        }
    }
}

// MARK: - Subviews

struct UserHeaderView: View {
    let userName: String
    let isLoading: Bool
    var onProfileClick: () -> Void
    var onHelpClick: () -> Void
    var onShareClick: () -> Void
    var onRateUsClick: () -> Void
    
    @State private var showMenu = false
    
    var body: some View {
        HStack {
            Button(action: onProfileClick) {
                HStack(spacing: 12) {
                    Image(systemName: "person.crop.circle.fill")
                        .resizable()
                        .frame(width: 40, height: 40)
                        .foregroundColor(.white)
                    
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Welcome back,")
                            .font(.system(size: 12))
                            .foregroundColor(.white.opacity(0.7))
                        Text(userName.isEmpty ? "Investor" : userName)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                    }
                }
            }
            
            Spacer()
            
            HStack(spacing: 16) {
                Button(action: onHelpClick) {
                    Image(systemName: "questionmark.circle")
                        .font(.system(size: 22))
                        .foregroundColor(.white)
                }
                
                Button(action: { showMenu.toggle() }) {
                    Image(systemName: "ellipsis.circle")
                        .font(.system(size: 22))
                        .foregroundColor(.white)
                }
                .sheet(isPresented: $showMenu) {
                    VStack(spacing: 20) {
                        Text("Menu Options")
                            .font(.headline)
                            .padding(.top)
                        
                        Button("Share App") {
                            showMenu = false
                            onShareClick()
                        }
                        .font(.body)
                        
                        Button("Rate Us") {
                            showMenu = false
                            onRateUsClick()
                        }
                        .font(.body)
                        
                        Button("Cancel", role: .cancel) {
                            showMenu = false
                        }
                    }
                    .applyPresentationDetents()
                }
            }
        }
        .padding(.horizontal)
    }
}

struct CombinedDashboardCardView: View {
    let totalValue: Double
    let profitLoss: Double
    let profitLossPercentage: Double
    let isLoading: Bool
    
    var body: some View {
        VStack(spacing: 16) {
            VStack(spacing: 4) {
                Text("TOTAL PORTFOLIO VALUE")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(PyllarTheme.textGray)
                
                Text(String(format: "₹%.2f", totalValue))
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(PyllarTheme.textDark)
            }
            
            HStack(spacing: 24) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Total Return")
                        .font(.system(size: 12))
                        .foregroundColor(PyllarTheme.textGray)
                    Text(String(format: "₹%.2f", profitLoss))
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(profitLoss >= 0 ? PyllarTheme.successGreen : .red)
                }
                
                Divider()
                    .frame(height: 30)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Absolute ROI")
                        .font(.system(size: 12))
                        .foregroundColor(PyllarTheme.textGray)
                    Text(String(format: "%.2f%%", profitLossPercentage))
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(profitLossPercentage >= 0 ? PyllarTheme.successGreen : .red)
                }
            }
        }
        .padding(.vertical, 24)
        .padding(.horizontal, 16)
        .frame(maxWidth: .infinity)
        .background(PyllarTheme.white)
        .cornerRadius(16)
        .shadow(color: Color.black.opacity(0.04), radius: 8, x: 0, y: 4)
        .padding(.horizontal)
    }
}

struct ActiveGoalCardView: View {
    let goal: InvestmentGoal
    var onClick: () -> Void
    
    var body: some View {
        Button(action: onClick) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(goal.name)
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(PyllarTheme.textDark)
                        Text(goal.schemeName ?? "Mutual Fund")
                            .font(.system(size: 12))
                            .foregroundColor(PyllarTheme.textGray)
                    }
                    
                    Spacer()
                    
                    Text(String(format: "₹%.2f", goal.currentValue))
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(PyllarTheme.textDark)
                }
                
                // Progress Bar
                VStack(spacing: 4) {
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            RoundedRectangle(cornerRadius: 4)
                                .fill(PyllarTheme.border)
                                .frame(height: 8)
                            
                            RoundedRectangle(cornerRadius: 4)
                                .fill(PyllarTheme.successGreen)
                                .frame(width: geo.size.width * CGFloat(min(max(goal.progressPercentage / 100.0, 0.0), 1.0)), height: 8)
                        }
                    }
                    .frame(height: 8)
                    
                    HStack {
                        Text(String(format: "%.1f%% achieved", goal.progressPercentage))
                            .font(.system(size: 11))
                            .foregroundColor(PyllarTheme.textGray)
                        Spacer()
                        Text(String(format: "Target: ₹%.0f", goal.targetAmount))
                            .font(.system(size: 11))
                            .foregroundColor(PyllarTheme.textGray)
                    }
                }
            }
            .padding(16)
            .background(PyllarTheme.white)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(PyllarTheme.border, lineWidth: 1)
            )
        }
    }
}

struct RecommendedGoalCard: View {
    let goal: InvestmentGoal
    var onClick: () -> Void
    
    var body: some View {
        Button(action: onClick) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Image(systemName: "star.fill")
                        .foregroundColor(.orange)
                        .padding(8)
                        .background(Color.orange.opacity(0.1))
                        .clipShape(Circle())
                    
                    Spacer()
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(goal.name)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(PyllarTheme.textDark)
                        .lineLimit(1)
                    Text(goal.description_)
                        .font(.system(size: 11))
                        .foregroundColor(PyllarTheme.textGray)
                        .lineLimit(2)
                }
                
                Text(goal.actionButtonText.isEmpty ? "Invest Now" : goal.actionButtonText)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.vertical, 8)
                    .frame(maxWidth: .infinity)
                    .background(PyllarTheme.obsidian)
                    .cornerRadius(8)
            }
            .padding(16)
            .frame(width: 160)
            .background(PyllarTheme.white)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(PyllarTheme.border, lineWidth: 1)
            )
        }
    }
}

struct KycStatusBanner: View {
    let status: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundColor(.orange)
                .font(.title2)
            
            VStack(alignment: .leading, spacing: 2) {
                Text("KYC Verification Required")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(PyllarTheme.textDark)
                Text("Status: \(status.capitalized). Click here to verify your KYC details and unlock full access.")
                    .font(.system(size: 11))
                    .foregroundColor(PyllarTheme.textGray)
            }
            Spacer()
        }
        .padding(16)
        .background(Color.orange.opacity(0.08))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color.orange.opacity(0.3), lineWidth: 1)
        )
        .padding(.horizontal)
    }
}

struct ReadyToInvestCard: View {
    var body: some View {
        VStack(spacing: 8) {
            Text("🎉 KYC Approved!")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(PyllarTheme.textDark)
            Text("Your account is active. Choose an investment goal below to get started.")
                .font(.system(size: 12))
                .foregroundColor(PyllarTheme.textGray)
                .multilineTextAlignment(.center)
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(PyllarTheme.white)
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(PyllarTheme.border, lineWidth: 1)
        )
    }
}

struct MilestoneCard: View {
    let message: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "trophy.fill")
                .foregroundColor(.yellow)
            Text(message)
                .font(.system(size: 12, weight: .medium))
                .foregroundColor(PyllarTheme.textDark)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.yellow.opacity(0.1))
        .cornerRadius(8)
    }
}

extension View {
    @ViewBuilder
    func applyPresentationDetents() -> some View {
        if #available(iOS 16.0, *) {
            self.presentationDetents([.fraction(0.25)])
        } else {
            self
        }
    }
}
