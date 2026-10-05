import re
from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.lib.units import mm
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer, Table, TableStyle, KeepTogether

Y=colors.HexColor("#F2C230"); B=colors.HexColor("#5C3A21"); G=colors.HexColor("#4A7C39"); BG=colors.HexColor("#FFFBEA")
FN="Radar_Natural_S41_2026-10-05.pdf"
body=ParagraphStyle("b",fontName="Helvetica",fontSize=9.6,leading=13.2,textColor=colors.HexColor("#2B2118"),spaceAfter=3)
h=ParagraphStyle("h",fontName="Helvetica-Bold",fontSize=11.5,leading=14,textColor=B,spaceBefore=8,spaceAfter=3)
bl=ParagraphStyle("bl",parent=body,leftIndent=10,bulletIndent=0,spaceAfter=1.5)
cell=ParagraphStyle("c",parent=body,fontSize=8.4,leading=10.6,spaceAfter=0)
cellh=ParagraphStyle("ch",parent=cell,fontName="Helvetica-Bold",textColor=B)
small=ParagraphStyle("s",parent=body,fontSize=7.8,leading=10,textColor=colors.HexColor("#5a4a3c"))
words=[]
def P(t,s=body): words.append(re.sub("<[^>]+>","",t)); return Paragraph(t,s)
def H(t):
    words.append(t)
    tb=Table([[Paragraph(t,h)]],colWidths=[174*mm]); tb.setStyle(TableStyle([("LINEBEFORE",(0,0),(0,0),4,Y),("BACKGROUND",(0,0),(-1,-1),BG),("LEFTPADDING",(0,0),(-1,-1),7),("TOPPADDING",(0,0),(-1,-1),1),("BOTTOMPADDING",(0,0),(-1,-1),1)]))
    return [Spacer(1,4),tb,Spacer(1,3)]
def BL(t): words.append(t); return Paragraph(re.sub(r"^","",t),bl,bulletText="•")

def page(c,d):
    c.saveState(); w,hh=A4
    c.setFillColor(Y); c.rect(0,hh-6*mm,w,6*mm,fill=1,stroke=0)
    c.setFillColor(G); c.rect(0,0,w,9*mm,fill=1,stroke=0)
    c.setFillColor(colors.white); c.setFont("Helvetica-Bold",8.5)
    c.drawString(18*mm,3.3*mm,"Loja da Banana · do sítio à sua prateleira"); c.drawRightString(w-18*mm,3.3*mm,"Radar Natural · pág. %d"%d.page)
    c.restoreState()

doc=BaseDocTemplate("out/"+FN,pagesize=A4,leftMargin=18*mm,rightMargin=18*mm,topMargin=14*mm,bottomMargin=15*mm,title="Radar Natural - Semana 41/2026",author="Loja da Banana")
doc.addPageTemplates([PageTemplate(id="p",frames=[Frame(18*mm,15*mm,174*mm,A4[1]-29*mm,id="f",leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0)],onPage=page)])
S=[]
words.append("Radar Natural Semana 41/2026 5 a 11 de outubro")
t=Table([[Paragraph('<font color="#5C3A21" size="22"><b>Radar Natural</b></font>',body),Paragraph('<font color="#5C3A21" size="10"><b>Semana 41/2026</b><br/>5 a 11 de outubro</font>',ParagraphStyle("r",parent=body,alignment=2,leading=13))]],colWidths=[110*mm,64*mm])
t.setStyle(TableStyle([("BACKGROUND",(0,0),(-1,-1),Y),("VALIGN",(0,0),(-1,-1),"MIDDLE"),("TOPPADDING",(0,0),(-1,-1),8),("BOTTOMPADDING",(0,0),(-1,-1),8),("LEFTPADDING",(0,0),(-1,-1),10),("RIGHTPADDING",(0,0),(-1,-1),10)]))
S+= [t,Spacer(1,2)]
S.append(P("<i>Informativo da Loja da Banana para lojistas de produtos naturais de Pernambuco e do Nordeste.</i>",small))

S+=H("1. O essencial em 30 segundos")
for x in ["Procura por alimentos mais saudáveis cresce no Nordeste: veja os números da Scanntech abaixo.",
"Simples Nacional: prazo para pedir ingresso e valer em 2027 vai até 15/10 (não vale para MEI).",
"HFN, em Olinda, de 4 a 6/11: credenciamento gratuito com CNPJ.",
"Black Friday é 27/11: comece a montar sua oferta por margem já."]: S.append(BL(x))

S+=H("2. Tendência da semana: o consumidor quer comida de verdade, com menos açúcar")
S.append(P("A busca por alimentos mais saudáveis segue em alta. Um levantamento da Scanntech mostra aumento na procura por frutas in natura (33,9%), ovos (24,3%) e queijos (17,3%), no país e em estados do Nordeste (Scanntech, via Frances News, jul/2026). Na região, as categorias que mais crescem são as de menos açúcar ou zero açúcar, integrais, proteicos e feitos com ingredientes naturais. A tendência ganha força entre os consumidores mais jovens, e os supermercados já estão dando mais espaço a esses itens (Frances News, jul/2026)."))
S.append(P("O recado para a loja de bairro é simples: quem entra procurando “algo mais leve” precisa achar isso rápido. Se o cliente tem de perguntar onde está, você perde a venda para o supermercado, que já está reorganizando a gôndola. Você tem uma vantagem que a rede não tem: conversa. Explique o que é cada produto e deixe provar."))
S.append(P("<b>O que fazer na sua loja:</b> (1) Reúna numa só prateleira, com placa, os produtos de lista de ingredientes curta e sem açúcar adicionado, conferindo o rótulo item a item antes de anunciar. (2) Escolha um produto por semana para degustação no balcão e anote quantas unidades saem no dia."))

S+=H("3. Radar de regras")
S.append(BL("<b>Simples Nacional.</b> Empresas que ainda não são optantes podem pedir ingresso até 15/10/2026 para valer a partir de janeiro de 2027. O prazo, antes em setembro, foi prorrogado. A regra não vale para MEI, que segue opção em janeiro (Agência Brasil, set/2026). <i>Faça:</i> se você é ME ou EPP fora do Simples, fale com seu contador esta semana."))
S.append(BL("<b>Alergênicos (proposta, ainda não é regra).</b> A Anvisa colocou em consulta pública, encerrada em 9/3/2026, a inclusão de gergelim, trigo-sarraceno, aipo, tremoço e mostarda e um aviso padronizado “ATENÇÃO: Contém…” (Consultoria GMC, nov/2025). Não encontramos norma final publicada. <i>Faça:</i> peça aos fornecedores a ficha de rotulagem dos produtos e arquive."))
S.append(BL("<b>Já vale:</b> a RDC 727/2022 unificou as regras de rotulagem geral e de alergênicos (Hdrup, 2026). <i>Faça:</i> se você reembala produto, revise o rótulo do seu pacote com seu fornecedor ou com a vigilância sanitária local."))

S+=H("4. Agenda")
rows=[["Evento","Data","Cidade","Para quem serve","Como participar"],
["HFN – Hotel & Food Nordeste","4 a 6/11/2026, 14h às 21h","Olinda (PE), Centro de Convenções","Quem compra e busca fornecedores de alimentos","Credenciamento grátis com CNPJ; CPF sem vínculo com o setor paga R$ 50 (HFN/Folha PE)"],
["17ª Feira Baiana da Agricultura Familiar e Economia Solidária","2 a 6/12/2026","Salvador (BA), Parque Costa Azul","Lojista que quer conhecer produtores e produtos regionais","Visita. Para expor, só via entidade territorial (edital; inscrições até 15/10) (Bahia Econômica, set/2026)"],
["Naturaltech / Bio Brazil Fair","9 a 12/6/2027","São Paulo (SP), Anhembi","Quem acompanha lançamentos do setor natural e orgânico","Planejamento de longo prazo; credenciamento no site oficial (Neventum, 2026)"]]
data=[[Paragraph(c,cellh if i==0 else cell) for c in r] for i,r in enumerate(rows)]
for r in rows: words.append(" ".join(r))
tb=Table(data,colWidths=[38*mm,27*mm,31*mm,35*mm,43*mm],repeatRows=1)
tb.setStyle(TableStyle([("BACKGROUND",(0,0),(-1,0),Y),("ROWBACKGROUNDS",(0,1),(-1,-1),[colors.white,BG]),("GRID",(0,0),(-1,-1),0.4,colors.HexColor("#d8c9a0")),("VALIGN",(0,0),(-1,-1),"TOP"),("TOPPADDING",(0,0),(-1,-1),3),("BOTTOMPADDING",(0,0),(-1,-1),3)]))
S.append(tb)

S+=H("5. Calendário comercial")
for x in ["<b>12/10, Dia das Crianças:</b> monte combos de lanche com fruta desidratada e barrinha, com preço fechado.",
"<b>16/10, Dia Mundial da Alimentação:</b> publique um post sobre a origem dos seus produtos e de onde vêm as frutas.",
"<b>1/11, Dia Mundial do Vegano:</b> destaque a prateleira plant-based com placa e uma sugestão de uso.",
"<b>27/11, Black Friday (Cyber Monday em 30/11):</b> defina a oferta pela margem, não pelo percentual; some custo, embalagem e margem mínima antes de dar desconto (Muito Mais Digital, 2026).",
"<b>Natal:</b> no início de novembro, monte kits por faixa de preço, com prazo de entrega claro."]: S.append(BL(x))

S+=H("6. Dica prática da semana: curva ABC em 7 dias")
S.append(P("Método recomendado pelo Sebrae: itens A respondem por até 80% das vendas, B por até 15% e C por até 5% (Sebrae, 2026). Custo: zero."))
for i,x in enumerate(["Dia 1: liste as vendas dos últimos 90 dias, por produto, em caderno ou planilha.","Dia 2: ordene do que mais vendeu para o que menos vendeu.","Dia 3: marque A, B e C conforme os percentuais acima.","Dia 4: itens A nunca podem faltar; ponha-os na altura dos olhos.","Dia 5: itens C viram combo ou promoção, e você para de repor.","Dias 6 e 7: aplique o PEPS: o que vence primeiro fica na frente e sai primeiro."],1): S.append(BL(x))

S+=H("7. Produto em foco: snacks de fruta")
S.append(P("O nicho de snacks à base de frutas movimentou mais de R$ 780 milhões no Brasil em 2023, com os chips de banana ganhando espaço (Ejeq, 2023). <b>Mix sugerido:</b> um item de entrada (unidade, para compra por impulso), um pacote de médio porte e um kit para presente. <b>Margem:</b> como referência nossa, e não dado de mercado, mire 40% a 50% de margem bruta no preço de venda e ajuste ao seu custo. <b>Produto nosso:</b> nossos snacks de banana entram nessa categoria; peça a tabela pelo WhatsApp."))

S+=H("8. Da nossa cozinha")
S.append(P("Nossa banana nasce no cultivo próprio, em Palmares, e vira produto na fábrica de Caruaru. A venda é direta, sem intermediário. Pedidos e tabela de preços: responda esta mensagem no WhatsApp."))

S+=H("9. Fale com a gente")
S.append(P("<b>WhatsApp:</b> responda esta mensagem com o tema que você quer ver na próxima edição."))
S.append(P("<b>Fontes consultadas:</b> Frances News/Scanntech (jul/2026); Agência Brasil (set/2026); Consultoria GMC (nov/2025); Hdrup (2026); A3 Analítica (2026); HFN/Folha PE; Bahia Notícias e Bahia Econômica (set/2026); Neventum (2026); Sebrae (2026); Muito Mais Digital (2026); Ejeq (2023). Evento sem data e cidade confirmadas ficou de fora.",small))
doc.build(S)
n=len(" ".join(words).split()); print("words",n)
