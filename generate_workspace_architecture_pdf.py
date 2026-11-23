import os
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.lib.units import inch
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether, HRFlowable
)
from reportlab.pdfgen import canvas

class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super(NumberedCanvas, self).__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            super(NumberedCanvas, self).showPage()
        super(NumberedCanvas, self).save()

    def draw_page_decorations(self, page_count):
        self.saveState()
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor("#64748B"))
        
        # Header (Pages > 1)
        if self._pageNumber > 1:
            self.drawString(36, 11 * inch - 26, "Nawano Inventory & POS • Product Strategy & Workspace Separation Architecture")
            self.drawRightString(8.5 * inch - 36, 11 * inch - 26, "Individual vs. Partner Mode • Multi-Tenant Design")
            self.setStrokeColor(colors.HexColor("#E2E8F0"))
            self.setLineWidth(0.5)
            self.line(36, 11 * inch - 30, 8.5 * inch - 36, 11 * inch - 30)
            
        # Footer (All pages)
        self.setStrokeColor(colors.HexColor("#E2E8F0"))
        self.setLineWidth(0.5)
        self.line(36, 32, 8.5 * inch - 36, 32)
        
        self.drawString(36, 20, "Confidential • Nawano Engineering Architecture Document • Project Inventory POS")
        self.drawRightString(8.5 * inch - 36, 20, f"Page {self._pageNumber} of {page_count}")
        self.restoreState()

def build_pdf(filename="Nawano_Individual_vs_Partner_Workspace_Guide.pdf"):
    doc = SimpleDocTemplate(
        filename,
        pagesize=letter,
        leftMargin=36,
        rightMargin=36,
        topMargin=38,
        bottomMargin=38
    )

    styles = getSampleStyleSheet()
    
    # Custom Palette
    c_primary = colors.HexColor('#7871F8')
    c_primary_dark = colors.HexColor('#4F46E5')
    c_dark = colors.HexColor('#0F172A')
    c_slate = colors.HexColor('#475569')
    c_light_bg = colors.HexColor('#F8FAFC')
    c_success = colors.HexColor('#059669')
    c_warning = colors.HexColor('#D97706')
    c_danger = colors.HexColor('#DC2626')

    # Typography Styles
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=17,
        leading=21,
        textColor=c_dark
    )
    
    subtitle_style = ParagraphStyle(
        'DocSubtitle',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9,
        leading=13,
        textColor=c_slate
    )

    h1_style = ParagraphStyle(
        'Heading1',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=11.5,
        leading=15,
        textColor=c_primary_dark,
        spaceBefore=10,
        spaceAfter=4
    )

    h2_style = ParagraphStyle(
        'Heading2',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=9.5,
        leading=13,
        textColor=c_dark,
        spaceBefore=6,
        spaceAfter=2
    )

    body_style = ParagraphStyle(
        'Body',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8.3,
        leading=11.5,
        textColor=c_dark
    )

    body_bold = ParagraphStyle(
        'BodyBold',
        parent=body_style,
        fontName='Helvetica-Bold'
    )

    table_header_style = ParagraphStyle(
        'TableHeader',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=7.8,
        leading=10,
        textColor=colors.white
    )

    cell_style = ParagraphStyle(
        'TableCell',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=7.6,
        leading=10,
        textColor=c_dark
    )

    cell_bold = ParagraphStyle(
        'TableCellBold',
        parent=cell_style,
        fontName='Helvetica-Bold'
    )

    cell_code = ParagraphStyle(
        'TableCellCode',
        parent=cell_style,
        fontName='Courier',
        fontSize=7.0,
        leading=9.2
    )

    badge_success = ParagraphStyle(
        'BadgeSuccess',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=7.2,
        leading=9.5,
        textColor=colors.HexColor('#166534'),
        alignment=1
    )

    badge_warning = ParagraphStyle(
        'BadgeWarning',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=7.2,
        leading=9.5,
        textColor=colors.HexColor('#92400E'),
        alignment=1
    )

    badge_danger = ParagraphStyle(
        'BadgeDanger',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=7.2,
        leading=9.5,
        textColor=colors.HexColor('#991B1B'),
        alignment=1
    )

    badge_primary = ParagraphStyle(
        'BadgePrimary',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=7.2,
        leading=9.5,
        textColor=colors.HexColor('#4338CA'),
        alignment=1
    )

    story = []

    # =========================================================================
    # PAGE 1: EXECUTIVE PROPOSAL & CORE STRATEGY VERDICT
    # =========================================================================
    
    # Header Banner
    header_content = [
        [
            Paragraph("<b>NAWANO INVENTORY & POS ARCHITECTURE</b>", title_style),
            Paragraph("<b>VERSION:</b> 2.4-PROPOSAL<br/><b>STATUS:</b> STRATEGIC BLUEPRINT", ParagraphStyle('HDate', parent=subtitle_style, alignment=2))
        ],
        [
            Paragraph("Individual vs. Partner Mode: Workspace Separation, Data Isolation & Switching UX", subtitle_style),
            Paragraph("<b>Stack:</b> Kotlin 2.0.21 • Jetpack Compose • Room • DataStore", ParagraphStyle('HPlat', parent=subtitle_style, alignment=2))
        ]
    ]
    header_table = Table(header_content, colWidths=[380, 160])
    header_table.setStyle(TableStyle([
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('BOTTOMPADDING', (0,0), (-1,-1), 2),
        ('TOPPADDING', (0,0), (-1,-1), 0),
        ('LEFTPADDING', (0,0), (-1,-1), 0),
        ('RIGHTPADDING', (0,0), (-1,-1), 0),
    ]))
    story.append(header_table)
    story.append(Spacer(1, 6))
    story.append(HRFlowable(width="100%", thickness=1.5, color=c_primary, spaceBefore=0, spaceAfter=8))

    # Executive Metrics Summary Cards
    summary_data = [
        [
            Paragraph("<b>RECOMMENDED MODEL</b><br/><font size=11 color='#059669'><b>HYBRID SWITCHER</b></font><br/><font size=6.5 color='#64748B'>Dynamic Context Switching</font>", ParagraphStyle('C1', parent=body_style, alignment=1)),
            Paragraph("<b>ONBOARDING PATH</b><br/><font size=11 color='#4F46E5'><b>PROGRESSIVE</b></font><br/><font size=6.5 color='#64748B'>Solo First, Connect Later</font>", ParagraphStyle('C2', parent=body_style, alignment=1)),
            Paragraph("<b>DATA ISOLATION</b><br/><font size=11 color='#7871F8'><b>ZERO LEAKAGE</b></font><br/><font size=6.5 color='#64748B'>Room Workspace Scoping</font>", ParagraphStyle('C3', parent=body_style, alignment=1)),
            Paragraph("<b>USER FRICTION</b><br/><font size=11 color='#059669'><b>ZERO RELOGGING</b></font><br/><font size=6.5 color='#64748B'>1-Tap Account Switcher</font>", ParagraphStyle('C4', parent=body_style, alignment=1)),
        ]
    ]
    summary_table = Table(summary_data, colWidths=[135, 135, 135, 135])
    summary_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F1F5F9')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#CBD5E1')),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
        ('TOPPADDING', (0,0), (-1,-1), 6),
        ('BOTTOMPADDING', (0,0), (-1,-1), 6),
        ('VALIGN', (0,0), (-1,-1), 'MIDDLE'),
    ]))
    story.append(summary_table)
    story.append(Spacer(1, 10))

    # Section 1: The Core Question - Forced Choice vs. Dynamic Switching
    story.append(Paragraph("1. Strategic Question: One-Time Hard Choice vs. Dynamic Switching?", h1_style))
    story.append(Paragraph("Evaluating whether the app should force a user to choose 'Individual' or 'Partner' at setup, versus offering seamless switching between workspaces.", subtitle_style))
    story.append(Spacer(1, 4))

    strategy_table_data = [
        [
            Paragraph("Evaluation Criteria", table_header_style),
            Paragraph("Option A: Force Choice at Signup", table_header_style),
            Paragraph("Option B: Dynamic Mode Switching (Recommended)", table_header_style),
            Paragraph("Architecture Impact", table_header_style)
        ],
        [
            Paragraph("<b>User Flexibility</b>", cell_bold),
            Paragraph("Rigid. User is locked into solo or partner mode. If they need both, they must create 2 accounts.", cell_style),
            Paragraph("<b>Maximum Flexibility:</b> User switches between 👤 Personal and 🤝 Partner with 1 tap, like Slack/Shopify.", cell_style),
            Paragraph("<b>WINNER (Option B)</b>", badge_success)
        ],
        [
            Paragraph("<b>Business Growth</b>", cell_bold),
            Paragraph("Fails when a solo merchant expands and partners with a supplier or co-owner.", cell_style),
            Paragraph("<b>Scales Naturally:</b> A solo shop owner connects a partner anytime via Unique Code (e.g. <code>MID-1001</code>).", cell_style),
            Paragraph("<b>FUTURE PROOF</b>", badge_primary)
        ],
        [
            Paragraph("<b>Onboarding Speed</b>", cell_bold),
            Paragraph("Forces complex questions during registration before the user even understands the app.", cell_style),
            Paragraph("<b>Frictionless:</b> User enters immediate Solo mode. Partner connection is progressive disclosure.", cell_style),
            Paragraph("<b>HIGHER CONVERSION</b>", badge_success)
        ],
        [
            Paragraph("<b>Data Contamination</b>", cell_bold),
            Paragraph("Separation is absolute only because accounts are separate, causing massive log-in fatigue.", cell_style),
            Paragraph("<b>Engineered Separation:</b> Room queries strictly filter by <code>workspace_id</code>. Zero data cross-leakage.", cell_style),
            Paragraph("<b>ENTERPRISE GRADE</b>", badge_success)
        ],
        [
            Paragraph("<b>Human Error Risk</b>", cell_bold),
            Paragraph("Low, but at the cost of requiring multiple devices or constant email logouts.", cell_style),
            Paragraph("Prevented via <b>persistent visual branding</b> (accent colors, workspace badge chips, top-bar indicator).", cell_style),
            Paragraph("<b>INTUITIVE UX</b>", badge_primary)
        ]
    ]

    t_strategy = Table(strategy_table_data, colWidths=[90, 160, 210, 80])
    t_strategy.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_dark),
        ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('TOPPADDING', (0,0), (-1,-1), 4.5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4.5),
        ('BACKGROUND', (3,1), (3,1), colors.HexColor('#DCFCE7')),
        ('BACKGROUND', (3,2), (3,2), colors.HexColor('#EEF2FF')),
        ('BACKGROUND', (3,3), (3,3), colors.HexColor('#DCFCE7')),
        ('BACKGROUND', (3,4), (3,4), colors.HexColor('#DCFCE7')),
        ('BACKGROUND', (3,5), (3,5), colors.HexColor('#EEF2FF')),
    ]))
    story.append(t_strategy)
    story.append(Spacer(1, 8))

    # Strategic Takeaway Box
    takeaway_content = [
        [
            Paragraph(
                "<b>Strategic Takeaway:</b> Never trap users behind an irreversible setup choice. Real business owners operate in multiple contexts simultaneously—for example, managing their private retail stock while also fulfilling consignment or wholesale orders for an upstream partner. <b>Dynamic Workspace Switching</b> paired with <b>Progressive Disclosure</b> provides the cleanest separation while retaining 100% of user agility.",
                cell_style
            )
        ]
    ]
    t_takeaway = Table(takeaway_content, colWidths=[540])
    t_takeaway.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#EFF6FF')),
        ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor('#BFDBFE')),
        ('TOPPADDING', (0,0), (-1,-1), 6),
        ('BOTTOMPADDING', (0,0), (-1,-1), 6),
        ('LEFTPADDING', (0,0), (-1,-1), 8),
        ('RIGHTPADDING', (0,0), (-1,-1), 8),
    ]))
    story.append(t_takeaway)

    # Section 2: Real-World User Personas
    story.append(Spacer(1, 9))
    story.append(Paragraph("2. Real-World Personas & Why Separation Must Be Easy", h1_style))
    story.append(Paragraph("Three distinct user archetypes demonstrating how the app supports individual, partner, and hybrid workflows.", subtitle_style))
    story.append(Spacer(1, 4))

    personas_data = [
        [
            Paragraph("Persona Type", table_header_style),
            Paragraph("Operational Goal", table_header_style),
            Paragraph("Key Need & Privacy Requirement", table_header_style),
            Paragraph("App Experience", table_header_style)
        ],
        [
            Paragraph("<b>1. The Solo Merchant</b><br/><font size=6.5 color='#64748B'>Freelancer / Boutique</font>", cell_bold),
            Paragraph("Runs their own boutique, tracks items locally, rings POS sales, prints receipts.", cell_style),
            Paragraph("Wants zero bloat. No confusing supplier portals, no partner menus. Absolute privacy of inventory costs.", cell_style),
            Paragraph("Default <b>Individual Mode</b>. Clean, fast, zero clutter.", cell_style)
        ],
        [
            Paragraph("<b>2. The Connected Partner</b><br/><font size=6.5 color='#64748B'>Franchise / Wholesale</font>", cell_bold),
            Paragraph("Supplies items, fulfills purchase orders, manages shipments between warehouses.", cell_style),
            Paragraph("Requires shared visibility of dispatch status, requisition tracking, and mutual product catalog.", cell_style),
            Paragraph("Active <b>Partner Workspace</b> linked via code <code>MID-1001</code>.", cell_style)
        ],
        [
            Paragraph("<b>3. The Hybrid Retailer</b><br/><font size=6.5 color='#64748B'>Consignment / Multi-Store</font>", cell_bold),
            Paragraph("Sells their own private handcrafted products AND sells goods supplied by partner brands.", cell_style),
            Paragraph("<b>CRITICAL:</b> Personal margin data & independent stock must NEVER be visible to the external partner.", cell_style),
            Paragraph("<b>Instant Toggle:</b> Switches workspaces without ever logging out.", cell_style)
        ]
    ]

    t_personas = Table(personas_data, colWidths=[110, 150, 160, 120])
    t_personas.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_dark),
        ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('TOPPADDING', (0,0), (-1,-1), 4.5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4.5),
    ]))
    story.append(t_personas)

    # =========================================================================
    # PAGE 2: USER EXPERIENCE & PROGRESSIVE ONBOARDING FLOW
    # =========================================================================
    story.append(PageBreak())

    story.append(Paragraph("3. Progressive Disclosure: Zero-Friction Onboarding Lifecycle", h1_style))
    story.append(Paragraph("How the app introduces complexity only when needed, avoiding intimidating setup steps.", subtitle_style))
    story.append(Spacer(1, 4))

    lifecycle_data = [
        [
            Paragraph("Stage", table_header_style),
            Paragraph("User Action", table_header_style),
            Paragraph("System State", table_header_style),
            Paragraph("User Interface Displayed", table_header_style)
        ],
        [
            Paragraph("<b>Phase 1: Day 1</b><br/><font size=6.5 color='#64748B'>Instant Entry</font>", cell_bold),
            Paragraph("Signs up with Name, Email, Password.", cell_style),
            Paragraph("Generates internal <code>Personal Workspace</code>. Assigns unique code for future sharing.", cell_style),
            Paragraph("Pure solo view: Inventory, POS Register, Quick Add, Categories, Sales History. No partner noise.", cell_style)
        ],
        [
            Paragraph("<b>Phase 2: Growth</b><br/><font size=6.5 color='#64748B'>Partner Discovery</font>", cell_bold),
            Paragraph("User decides to collaborate with a supplier or branch manager.", cell_style),
            Paragraph("Taps <b>'Connect Partner'</b> in Drawer or enters Partner's code in Profile.", cell_style),
            Paragraph("Presents a clean swipe-up modal with QR code scanner and <code>Unique Code</code> input field.", cell_style)
        ],
        [
            Paragraph("<b>Phase 3: Dual Mode</b><br/><font size=6.5 color='#64748B'>Workspace Active</font>", cell_bold),
            Paragraph("Connection confirmed with partner (e.g., Nawano HQ).", cell_style),
            Paragraph("App creates a scoped <code>Partner Workspace</code> in local DataStore & Room DB.", cell_style),
            Paragraph("<b>Workspace Switcher Header</b> activates in Drawer & Top Bar. Mode toggles instantly.", cell_style)
        ]
    ]

    t_lifecycle = Table(lifecycle_data, colWidths=[90, 140, 140, 170])
    t_lifecycle.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_dark),
        ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('TOPPADDING', (0,0), (-1,-1), 5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 5),
    ]))
    story.append(t_lifecycle)
    story.append(Spacer(1, 8))

    # Visual UI Blueprint: Workspace Switcher in Drawer
    story.append(Paragraph("4. UI Blueprint: The Navigation Drawer Workspace Switcher", h1_style))
    story.append(Paragraph("Exact layout specification for the top section of InventoryDrawer.kt:", subtitle_style))
    story.append(Spacer(1, 4))

    drawer_mockup_data = [
        [
            Paragraph("<b>NAVIGATION DRAWER: WORKSPACE SWITCHER SPECIFICATION</b>", ParagraphStyle('DMTitle', parent=body_bold, textColor=colors.white))
        ],
        [
            Paragraph(
                """<b>[ HEADER: Profile & Current Active Store ]</b><br/>
┌────────────────────────────────────────────────────────────────────────┐<br/>
│ &nbsp;<b>ACTIVE WORKSPACE</b> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;[ Tap to Switch ▾ ] │<br/>
│ &nbsp;<font color='#4F46E5'><b>● 👤 My Personal Store</b></font> &nbsp;<font color='#64748B'>• Private Stock (42 items)</font> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>[ ACTIVE ]</b> │<br/>
└────────────────────────────────────────────────────────────────────────┘<br/>
<br/>
<b>[ WORKSPACE SWITCHER EXPANDED DROPDOWN / BOTTOM SHEET ]</b><br/>
┌────────────────────────────────────────────────────────────────────────┐<br/>
│ &nbsp;<b>Select Active Store / Context:</b> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;│<br/>
│ &nbsp;[✓] <b>👤 Personal Inventory</b> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font color='#059669'>Solo Mode • Private Stock</font> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;│<br/>
│ &nbsp;[ &nbsp;] <b>🤝 Nawano Distribution HQ</b> &nbsp;&nbsp;<font color='#7871F8'>Partner Code: MID-1001 • Inbound (12)</font> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;│<br/>
│ &nbsp;[ &nbsp;] <b>🏢 Apex Wholesale Hub</b> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font color='#7871F8'>Partner Code: APX-9042 • Connected</font> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;│<br/>
│ &nbsp;--------------------------------------------------------------------- │<br/>
│ &nbsp;<b>➕ [ + Connect New Business Partner or Store ]</b> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;│<br/>
└────────────────────────────────────────────────────────────────────────┘
                """,
                cell_code
            )
        ]
    ]
    t_drawer_mockup = Table(drawer_mockup_data, colWidths=[540])
    t_drawer_mockup.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_dark),
        ('BACKGROUND', (0,1), (-1,1), colors.HexColor('#F8FAFC')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#CBD5E1')),
        ('TOPPADDING', (0,0), (-1,-1), 5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 5),
        ('LEFTPADDING', (0,0), (-1,-1), 8),
        ('RIGHTPADDING', (0,0), (-1,-1), 8),
    ]))
    story.append(t_drawer_mockup)
    story.append(Spacer(1, 8))

    # Section 5: The 3 Safeguards to Prevent User Confusion
    story.append(Paragraph("5. Three UI Safeguards to Prevent Human Separation Mistakes", h1_style))
    story.append(Paragraph("How the user interface ensures users never accidentally edit or sell the wrong inventory.", subtitle_style))
    story.append(Spacer(1, 4))

    safeguards_data = [
        [
            Paragraph("Safeguard Layer", table_header_style),
            Paragraph("Visual & Interactive Behavior", table_header_style),
            Paragraph("Benefit & Error Prevention", table_header_style)
        ],
        [
            Paragraph("<b>1. Top Bar Status Pill</b><br/><font size=6.5 color='#64748B'>Persistent Visibility</font>", cell_bold),
            Paragraph("Every dashboard screen has a pinned status pill: <code>[ 👤 Personal ]</code> or <code>[ 🤝 Partner: Nawano HQ ]</code>.", cell_style),
            Paragraph("User always knows where their edits are going without checking settings.", cell_style)
        ],
        [
            Paragraph("<b>2. Contextual Accent Colors</b><br/><font size=6.5 color='#64748B'>Subconscious Recognition</font>", cell_bold),
            Paragraph("Personal mode uses clean neutral slate (#475569) borders. Partner mode activates purple (#7871F8) branding & partner badge.", cell_style),
            Paragraph("Color psychology prevents accidental sales ring-ups on partner consignment stock.", cell_style)
        ],
        [
            Paragraph("<b>3. Action Boundary Guard</b><br/><font size=6.5 color='#64748B'>Confirmation Prompts</font>", cell_bold),
            Paragraph("Attempting to assign a personal item to a partner requisition displays a clear confirmation dialog.", cell_style),
            Paragraph("Prevents inadvertent leakage of private stock to partner logistics records.", cell_style)
        ]
    ]

    t_safeguards = Table(safeguards_data, colWidths=[130, 240, 170])
    t_safeguards.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_dark),
        ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('TOPPADDING', (0,0), (-1,-1), 5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 5),
    ]))
    story.append(t_safeguards)

    # =========================================================================
    # PAGE 3: TECHNICAL ARCHITECTURE & DATA ISOLATION BLUEPRINT
    # =========================================================================
    story.append(PageBreak())

    story.append(Paragraph("6. Technical Architecture: Strict Multi-Tenant Data Isolation", h1_style))
    story.append(Paragraph("Under-the-hood database scoping in Room / SQLite and Kotlin StateFlow state management.", subtitle_style))
    story.append(Spacer(1, 4))

    # Architecture Overview Callout
    tech_callout = [
        [
            Paragraph(
                "<b>How Data Isolation Works Under the Hood:</b> The application uses a single local database, but every entity is tagged with a <code>workspace_id</code>. When a user is in Personal Mode, <code>workspace_id = 'PERSONAL'</code>. When switched to a Partner, <code>workspace_id = 'PARTNER_MID1001'</code>. Room DAOs observe the active workspace Flow from <code>AppSessionStore</code>, ensuring queries <b>physically cannot return items from another workspace</b>.",
                cell_style
            )
        ]
    ]
    t_tech_callout = Table(tech_callout, colWidths=[540])
    t_tech_callout.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F1F5F9')),
        ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('TOPPADDING', (0,0), (-1,-1), 6),
        ('BOTTOMPADDING', (0,0), (-1,-1), 6),
        ('LEFTPADDING', (0,0), (-1,-1), 8),
        ('RIGHTPADDING', (0,0), (-1,-1), 8),
    ]))
    story.append(t_tech_callout)
    story.append(Spacer(1, 8))

    # Room Entity & DAO Code Blueprint
    story.append(Paragraph("7. Code Specification: Entities & DAO Query Filtering", h1_style))
    story.append(Paragraph("Modifications required in Room entities to enforce 100% data boundary integrity:", subtitle_style))
    story.append(Spacer(1, 4))

    code_data = [
        [
            Paragraph("<b>KOTLIN & ROOM SCHEMA SPECIFICATION</b>", ParagraphStyle('CDTitle', parent=body_bold, textColor=colors.white))
        ],
        [
            Paragraph(
                """<font color='#4F46E5'><b>// 1. Workspace Context Enum & Data Class</b></font><br/>
<font color='#0F172A'><b>enum class</b></font> <font color='#7871F8'>WorkspaceType</font> { PERSONAL, PARTNER }<br/>
<font color='#0F172A'><b>data class</b></font> <font color='#7871F8'>WorkspaceContext</font>(<br/>
&nbsp;&nbsp;&nbsp;&nbsp;<font color='#0F172A'><b>val</b></font> id: String, <font color='#64748B'>// "PERSONAL" or partner connection ID</font><br/>
&nbsp;&nbsp;&nbsp;&nbsp;<font color='#0F172A'><b>val</b></font> name: String, <font color='#64748B'>// "My Personal Store" or "Nawano HQ"</font><br/>
&nbsp;&nbsp;&nbsp;&nbsp;<font color='#0F172A'><b>val</b></font> type: WorkspaceType,<br/>
&nbsp;&nbsp;&nbsp;&nbsp;<font color='#0F172A'><b>val</b></font> partnerCode: String = ""<br/>
)<br/>
<br/>
<font color='#4F46E5'><b>// 2. Updated Room Product Entity with Workspace Tag</b></font><br/>
<font color='#0F172A'><b>@Entity</b></font>(tableName = "products", indices = [Index("workspace_id"), Index("barcode")])<br/>
<font color='#0F172A'><b>data class</b></font> <font color='#7871F8'>ProductEntity</font>(<br/>
&nbsp;&nbsp;&nbsp;&nbsp;<font color='#0F172A'><b>@PrimaryKey</b></font> <font color='#0F172A'><b>val</b></font> id: String,<br/>
&nbsp;&nbsp;&nbsp;&nbsp;<font color='#0F172A'><b>@ColumnInfo</b></font>(name = "workspace_id") <font color='#0F172A'><b>val</b></font> workspaceId: String = "PERSONAL",<br/>
&nbsp;&nbsp;&nbsp;&nbsp;<font color='#0F172A'><b>val</b></font> name: String,<br/>
&nbsp;&nbsp;&nbsp;&nbsp;<font color='#0F172A'><b>val</b></font> barcode: String,<br/>
&nbsp;&nbsp;&nbsp;&nbsp;<font color='#0F172A'><b>val</b></font> stockQuantity: Int,<br/>
&nbsp;&nbsp;&nbsp;&nbsp;<font color='#0F172A'><b>val</b></font> unitPrice: Double<br/>
)<br/>
<br/>
<font color='#4F46E5'><b>// 3. Isolated DAO Queries (Physical Boundary Guarantee)</b></font><br/>
<font color='#0F172A'><b>@Dao</b></font><br/>
<font color='#0F172A'><b>interface</b></font> <font color='#7871F8'>ProductDao</font> {<br/>
&nbsp;&nbsp;&nbsp;&nbsp;<font color='#0F172A'><b>@Query</b></font>("SELECT * FROM products WHERE workspace_id = :workspaceId ORDER BY name ASC")<br/>
&nbsp;&nbsp;&nbsp;&nbsp;<font color='#0F172A'><b>fun</b></font> observeProductsByWorkspace(workspaceId: String): Flow&lt;List&lt;ProductEntity&gt;&gt;<br/>
}
                """,
                cell_code
            )
        ]
    ]
    t_code = Table(code_data, colWidths=[540])
    t_code.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_dark),
        ('BACKGROUND', (0,1), (-1,1), colors.HexColor('#F8FAFC')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#CBD5E1')),
        ('TOPPADDING', (0,0), (-1,-1), 5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 5),
        ('LEFTPADDING', (0,0), (-1,-1), 8),
        ('RIGHTPADDING', (0,0), (-1,-1), 8),
    ]))
    story.append(t_code)
    story.append(Spacer(1, 8))

    # Section 8: Privacy Matrix
    story.append(Paragraph("8. Data Privacy Matrix: What is Shared vs. Strictly Kept Private", h1_style))
    story.append(Paragraph("Clear demarcation ensuring business partners cannot view confidential solo figures.", subtitle_style))
    story.append(Spacer(1, 4))

    privacy_data = [
        [
            Paragraph("Data Domain", table_header_style),
            Paragraph("In Personal Mode", table_header_style),
            Paragraph("In Partner Workspace", table_header_style),
            Paragraph("Cross-Access Status", table_header_style)
        ],
        [
            Paragraph("<b>Inventory Catalog</b>", cell_bold),
            Paragraph("Private SKUs, local products, raw cost.", cell_style),
            Paragraph("Partner items, shared catalog, replenishment.", cell_style),
            Paragraph("<b>STRICTLY SEPARATED</b>", badge_success)
        ],
        [
            Paragraph("<b>Sales & Revenue</b>", cell_bold),
            Paragraph("Personal POS daily cash/card sales and margin.", cell_style),
            Paragraph("Partner PO fulfillment and wholesale turnover.", cell_style),
            Paragraph("<b>STRICTLY PRIVATE</b>", badge_danger)
        ],
        [
            Paragraph("<b>Movements & Shipments</b>", cell_bold),
            Paragraph("Personal internal bay transfers & restocking.", cell_style),
            Paragraph("Shared <code>INBOUND</code> freight and dispatch receipts.", cell_style),
            Paragraph("<b>PARTNER VISIBLE</b>", badge_primary)
        ],
        [
            Paragraph("<b>Audit Ledger</b>", cell_bold),
            Paragraph("Private stock additions and manual write-offs.", cell_style),
            Paragraph("Shared barcode check-ins and delivery logs.", cell_style),
            Paragraph("<b>STRICTLY SEPARATED</b>", badge_success)
        ]
    ]

    t_privacy = Table(privacy_data, colWidths=[120, 150, 150, 120])
    t_privacy.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_dark),
        ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('TOPPADDING', (0,0), (-1,-1), 4.5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 4.5),
        ('BACKGROUND', (3,1), (3,1), colors.HexColor('#DCFCE7')),
        ('BACKGROUND', (3,2), (3,2), colors.HexColor('#FEE2E2')),
        ('BACKGROUND', (3,3), (3,3), colors.HexColor('#EEF2FF')),
        ('BACKGROUND', (3,4), (3,4), colors.HexColor('#DCFCE7')),
    ]))
    story.append(t_privacy)

    # =========================================================================
    # PAGE 4: IMPLEMENTATION ROADMAP & ACTION PLAN
    # =========================================================================
    story.append(PageBreak())

    story.append(Paragraph("9. Step-by-Step Implementation Roadmap", h1_style))
    story.append(Paragraph("Actionable engineering tasks to implement the Workspace Switcher in Nawano POS & Inventory.", subtitle_style))
    story.append(Spacer(1, 4))

    roadmap_data = [
        [
            Paragraph("Phase", table_header_style),
            Paragraph("Engineering Deliverables", table_header_style),
            Paragraph("Files Impacted", table_header_style),
            Paragraph("Complexity & Risk", table_header_style)
        ],
        [
            Paragraph("<b>Phase 1</b><br/><font size=6.5 color='#64748B'>State & Session</font>", cell_bold),
            Paragraph("• Add <code>activeWorkspaceId</code> to <code>AppSessionStore</code> (DataStore Preferences).<br/>• Create <code>WorkspaceContext</code> state flow in <code>InventoryViewModel</code>.", cell_style),
            Paragraph("<code>AppSessionStore.kt</code><br/><code>InventoryViewModel.kt</code>", cell_code),
            Paragraph("<b>LOW RISK</b><br/><font size=6.5 color='#059669'>Non-breaking default</font>", badge_success)
        ],
        [
            Paragraph("<b>Phase 2</b><br/><font size=6.5 color='#64748B'>DB Tagging</font>", cell_bold),
            Paragraph("• Room Migration: Add <code>workspace_id TEXT DEFAULT 'PERSONAL'</code> to products & sales.<br/>• Update DAOs with <code>WHERE workspace_id = :wsId</code>.", cell_style),
            Paragraph("<code>ProductEntity.kt</code><br/><code>ProductDao.kt</code><br/><code>InventoryDatabase.kt</code>", cell_code),
            Paragraph("<b>MEDIUM</b><br/><font size=6.5 color='#D97706'>Room Migration v2</font>", badge_warning)
        ],
        [
            Paragraph("<b>Phase 3</b><br/><font size=6.5 color='#64748B'>UI Switcher</font>", cell_bold),
            Paragraph("• Implement Workspace Switcher dropdown header in <code>InventoryDrawer.kt</code>.<br/>• Add persistent top-bar status pill in <code>InventoryTopBar.kt</code>.", cell_style),
            Paragraph("<code>InventoryDrawer.kt</code><br/><code>InventoryTopBar.kt</code>", cell_code),
            Paragraph("<b>LOW RISK</b><br/><font size=6.5 color='#059669'>UI Polish Only</font>", badge_success)
        ],
        [
            Paragraph("<b>Phase 4</b><br/><font size=6.5 color='#64748B'>Partner Connect</font>", cell_bold),
            Paragraph("• Polish 'Connect Partner' Bottom Sheet using unique code (e.g. <code>MID-1001</code>).<br/>• Auto-add connected partner to the workspace switcher list.", cell_style),
            Paragraph("<code>EditProfileBottomSheet.kt</code><br/><code>AuthRepository.kt</code>", cell_code),
            Paragraph("<b>LOW RISK</b><br/><font size=6.5 color='#059669'>Reuses existing flow</font>", badge_success)
        ]
    ]

    t_roadmap = Table(roadmap_data, colWidths=[80, 210, 160, 90])
    t_roadmap.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_dark),
        ('BOX', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E1')),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('TOPPADDING', (0,0), (-1,-1), 5),
        ('BOTTOMPADDING', (0,0), (-1,-1), 5),
    ]))
    story.append(t_roadmap)
    story.append(Spacer(1, 10))

    # Section 10: Final Recommendation Summary Box
    story.append(Paragraph("10. Executive Recommendation & Action Items", h1_style))
    story.append(Spacer(1, 4))

    exec_summary_box = [
        [
            Paragraph("<b>FINAL ARCHITECTURAL VERDICT & SUMMARY:</b>", ParagraphStyle('ESBTitle', parent=body_bold, textColor=colors.white))
        ],
        [
            Paragraph(
                """<b>1. DO NOT force a locked choice at signup:</b> Locking users creates friction, limits adoption, and forces multi-account workarounds.<br/>
<b>2. DO implement Dynamic Workspace Switching:</b> Follow the gold standard set by Slack and Shopify. Users start in Individual mode automatically, then toggle between Personal and Partner stores in 1 tap.<br/>
<b>3. GUARANTEE separation through UI + Data layers:</b>
&nbsp;&nbsp;&nbsp;&nbsp;• <b>UI:</b> Pinned top-bar indicator + color accents ensure users are always aware of their active store.<br/>
&nbsp;&nbsp;&nbsp;&nbsp;• <b>Data:</b> Room queries strictly scope items by <code>workspace_id</code>, guaranteeing private costs and profits never leak to business partners.<br/>
<b>4. NEXT STEP:</b> Proceed with Phase 1 & Phase 3 (DataStore active workspace state + Navigation Drawer Switcher).
                """,
                body_style
            )
        ]
    ]
    t_exec_summary = Table(exec_summary_box, colWidths=[540])
    t_exec_summary.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), c_primary_dark),
        ('BACKGROUND', (0,1), (-1,1), colors.HexColor('#F8FAFC')),
        ('BOX', (0,0), (-1,-1), 1, c_primary),
        ('TOPPADDING', (0,0), (-1,-1), 6),
        ('BOTTOMPADDING', (0,0), (-1,-1), 6),
        ('LEFTPADDING', (0,0), (-1,-1), 10),
        ('RIGHTPADDING', (0,0), (-1,-1), 10),
    ]))
    story.append(t_exec_summary)

    # Document Sign-off Footer Box
    story.append(Spacer(1, 14))
    signoff_data = [
        [
            Paragraph("<b>Prepared by:</b> Antigravity AI Engineering Architecture Team", subtitle_style),
            Paragraph("<b>Approved:</b> Core Platform Architecture Team", subtitle_style),
            Paragraph("<b>Status:</b> Ready for Implementation", ParagraphStyle('SStat', parent=subtitle_style, alignment=2))
        ]
    ]
    t_signoff = Table(signoff_data, colWidths=[200, 180, 160])
    t_signoff.setStyle(TableStyle([
        ('TOPPADDING', (0,0), (-1,-1), 2),
        ('BOTTOMPADDING', (0,0), (-1,-1), 2),
        ('LEFTPADDING', (0,0), (-1,-1), 0),
        ('RIGHTPADDING', (0,0), (-1,-1), 0),
    ]))
    story.append(t_signoff)

    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"Successfully generated PDF: {filename}")

if __name__ == '__main__':
    build_pdf("Nawano_Individual_vs_Partner_Workspace_Guide.pdf")
